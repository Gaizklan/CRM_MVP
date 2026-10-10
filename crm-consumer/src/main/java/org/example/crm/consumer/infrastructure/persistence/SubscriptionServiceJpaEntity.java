package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;

@Entity
@Table(name = "subscription_service")
public class SubscriptionServiceJpaEntity extends BrandScopedEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 64, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(nullable = false)
  private boolean active;

  protected SubscriptionServiceJpaEntity() {}

  static SubscriptionServiceJpaEntity create(SubscriptionService service) {
    SubscriptionServiceJpaEntity e = new SubscriptionServiceJpaEntity();
    e.id = service.id().value();
    e.code = service.code();
    e.assignBrand(service.brandId());
    e.apply(service);
    return e;
  }

  void apply(SubscriptionService service) {
    name = service.name();
    description = service.description();
    active = service.active();
  }

  SubscriptionService toDomain() {
    return SubscriptionService.restore(
        new SubscriptionServiceId(id), getBrandId(), code, name, description, active);
  }
}
