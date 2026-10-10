package org.example.crm.tracking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.consumer.domain.ConsumerId;
import org.junit.jupiter.api.Test;

class UserEventTest {

  private final BrandId brand = BrandId.random();
  private final ConsumerId consumer = ConsumerId.random();
  private final Instant now = Instant.parse("2026-10-10T10:00:00Z");

  @Test
  void allRequiredTypesExist() {
    assertThat(UserEventType.values())
        .extracting(Enum::name)
        .containsExactly(
            "LOGIN",
            "LOGOUT",
            "SERVICE_CLICK",
            "EXPERIENCE_CHANGED",
            "EMAIL_SENT",
            "EMAIL_OPENED",
            "EMAIL_CLICKED",
            "PREFERENCES_CHANGED");
    assertThat(UserEventType.parse("EMAIL_OPENED")).contains(UserEventType.EMAIL_OPENED);
    assertThat(UserEventType.parse("email_opened")).isEmpty();
  }

  @Test
  void createsEventWithAttributes() {
    UserEvent e =
        UserEvent.of(brand, consumer, UserEventType.SERVICE_CLICK, now, Map.of("service", "s1"));

    assertThat(e.id()).isNotNull();
    assertThat(e.attributes()).containsEntry("service", "s1");
    assertThat(e.source()).isEmpty();
    assertThat(UserEvent.of(brand, consumer, UserEventType.LOGIN, now, null).attributes())
        .isEmpty();
  }

  @Test
  void validatesAttributes() {
    assertThatThrownBy(
            () -> UserEvent.of(brand, consumer, UserEventType.LOGIN, now, Map.of("bad key", "x")))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(
            () ->
                UserEvent.of(
                    brand, consumer, UserEventType.LOGIN, now, Map.of("k", "x".repeat(501))))
        .isInstanceOf(DomainException.class);
    Map<String, String> many = new HashMap<>();
    for (int i = 0; i <= UserEvent.MAX_ATTRIBUTES; i++) {
      many.put("k" + i, "v");
    }
    assertThatThrownBy(() -> UserEvent.of(brand, consumer, UserEventType.LOGIN, now, many))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void criteriaValidatesRange() {
    assertThat(new UserEventCriteria(consumer, UserEventType.LOGIN, now, now.plusSeconds(1)).type())
        .isEqualTo(UserEventType.LOGIN);
    assertThat(new UserEventCriteria(null, null, null, now).to()).isEqualTo(now);
    assertThatThrownBy(() -> new UserEventCriteria(null, null, now, now))
        .isInstanceOf(DomainException.class);
  }
}
