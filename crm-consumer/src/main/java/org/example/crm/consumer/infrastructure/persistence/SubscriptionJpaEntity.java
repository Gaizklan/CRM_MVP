package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionServiceId;

@Entity
@Table(name = "subscription")
public class SubscriptionJpaEntity extends BrandScopedEntity {

  @Id private UUID id;

  @Column(name = "consumer_id", nullable = false, updatable = false)
  private UUID consumerId;

  @Column(name = "service_id", nullable = false, updatable = false)
  private UUID serviceId;

  @Column(name = "subscribed_at", nullable = false, updatable = false)
  private Instant subscribedAt;

  @Column(name = "unsubscribed_at")
  private Instant unsubscribedAt;

  protected SubscriptionJpaEntity() {}

  static SubscriptionJpaEntity create(Subscription s) {
    SubscriptionJpaEntity e = new SubscriptionJpaEntity();
    e.id = s.id();
    e.assignBrand(s.brandId());
    e.consumerId = s.consumerId().value();
    e.serviceId = s.serviceId().value();
    e.subscribedAt = s.subscribedAt();
    e.apply(s);
    return e;
  }

  void apply(Subscription s) {
    unsubscribedAt = s.unsubscribedAt().orElse(null);
  }

  Subscription toDomain() {
    return Subscription.restore(
        id,
        getBrandId(),
        new ConsumerId(consumerId),
        new SubscriptionServiceId(serviceId),
        subscribedAt,
        unsubscribedAt);
  }
}
