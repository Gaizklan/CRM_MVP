package org.example.crm.consumer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

  private final Clock t0 = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final Clock t1 = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC);
  private final BrandId brand = BrandId.random();

  @Test
  void serviceValidation() {
    SubscriptionService s = SubscriptionService.create(brand, "newsletter", " Boletín ", null);

    assertThat(s.name()).isEqualTo("Boletín");
    assertThat(s.active()).isTrue();
    assertThat(s.brandId()).isEqualTo(brand);
    s.update("Boletín semanal", "desc", false);
    assertThat(s.active()).isFalse();
    assertThat(s.description()).isEqualTo("desc");
    assertThatThrownBy(() -> SubscriptionService.create(brand, "Bad Code", "x", null))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> SubscriptionService.create(brand, "ok", " ", null))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> SubscriptionService.create(brand, "ok", "x", "d".repeat(1001)))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void subscribeAndCancelWithDates() {
    SubscriptionService service = SubscriptionService.create(brand, "news", "Novedades", null);
    ConsumerId consumer = ConsumerId.random();

    Subscription sub = Subscription.start(consumer, brand, service, t0);
    assertThat(sub.isActive()).isTrue();
    assertThat(sub.subscribedAt()).isEqualTo(t0.instant());
    assertThat(sub.unsubscribedAt()).isEmpty();
    assertThat(sub.serviceId()).isEqualTo(service.id());
    assertThat(sub.consumerId()).isEqualTo(consumer);

    sub.cancel(t1);
    sub.cancel(t0);
    assertThat(sub.isActive()).isFalse();
    assertThat(sub.unsubscribedAt()).contains(t1.instant());
  }

  @Test
  void cannotSubscribeToInactiveOrForeignService() {
    SubscriptionService inactive = SubscriptionService.create(brand, "old", "Antiguo", null);
    inactive.update("Antiguo", null, false);
    SubscriptionService foreign = SubscriptionService.create(BrandId.random(), "news", "N", null);

    assertThatThrownBy(() -> Subscription.start(ConsumerId.random(), brand, inactive, t0))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> Subscription.start(ConsumerId.random(), brand, foreign, t0))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void restore() {
    var id = java.util.UUID.randomUUID();
    var serviceId = SubscriptionServiceId.random();
    Subscription s =
        Subscription.restore(id, brand, ConsumerId.random(), serviceId, t0.instant(), t1.instant());

    assertThat(s.id()).isEqualTo(id);
    assertThat(s.brandId()).isEqualTo(brand);
    assertThat(s.isActive()).isFalse();
    assertThat(serviceId.toString()).isEqualTo(serviceId.value().toString());
  }
}
