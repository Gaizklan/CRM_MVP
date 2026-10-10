package org.example.crm.consumer.domain;

import java.time.Instant;
import java.util.Objects;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;

/** Evento de dominio: ha cambiado el consentimiento de un consumidor en un canal. */
public record ConsentChanged(
    BrandId brandId,
    ConsumerId consumerId,
    ConsentChannel channel,
    boolean granted,
    ConsentSource source,
    Instant occurredAt)
    implements DomainEvent {

  public ConsentChanged {
    Objects.requireNonNull(brandId, "brandId");
    Objects.requireNonNull(consumerId, "consumerId");
    Objects.requireNonNull(channel, "channel");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(occurredAt, "occurredAt");
  }

  static ConsentChanged of(ConsentChange change) {
    return new ConsentChanged(
        change.brandId(),
        change.consumerId(),
        change.channel(),
        change.granted(),
        change.source(),
        change.occurredAt());
  }
}
