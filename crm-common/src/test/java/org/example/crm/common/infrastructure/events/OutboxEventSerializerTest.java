package org.example.crm.common.infrastructure.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;
import org.junit.jupiter.api.Test;

class OutboxEventSerializerTest {

  public record SampleEvent(BrandId brandId, Instant occurredAt, String detail)
      implements DomainEvent {}

  private final OutboxEventSerializer serializer =
      new OutboxEventSerializer(
          new ObjectMapper()
              .registerModule(new JavaTimeModule())
              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

  @Test
  void roundTrip() {
    SampleEvent event =
        new SampleEvent(BrandId.random(), Instant.parse("2026-10-10T10:00:00Z"), "x");

    String type = serializer.typeOf(event);
    String json = serializer.serialize(event);

    assertThat(json).contains("2026-10-10T10:00:00Z");
    assertThat(serializer.deserialize(type, json)).isEqualTo(event);
  }

  @Test
  void rejectsUnknownOrNonEventTypes() {
    assertThatThrownBy(() -> serializer.deserialize("no.Existe", "{}"))
        .hasMessageContaining("desconocido");
    assertThatThrownBy(() -> serializer.deserialize(String.class.getName(), "\"x\""))
        .hasMessageContaining("no es un evento");
    assertThatThrownBy(() -> serializer.deserialize(DomainEvent.class.getName(), "{}"))
        .hasMessageContaining("no es un evento");
    assertThatThrownBy(() -> serializer.deserialize(SampleEvent.class.getName(), "{"))
        .hasMessageContaining("deserializar");
  }
}
