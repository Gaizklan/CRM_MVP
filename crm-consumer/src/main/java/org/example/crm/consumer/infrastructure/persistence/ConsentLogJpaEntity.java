package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.ConsumerId;
import org.hibernate.annotations.Immutable;

/** Registro de auditoría de consentimientos (solo inserción; la BD impide UPDATE/DELETE). */
@Entity
@Immutable
@Table(name = "consumer_consent_log")
public class ConsentLogJpaEntity extends BrandScopedEntity {

  @Id private UUID id;

  @Column(name = "consumer_id", nullable = false)
  private UUID consumerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ConsentChannel channel;

  @Column(nullable = false)
  private boolean granted;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ConsentSource source;

  @Column(nullable = false, length = 128)
  private String actor;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected ConsentLogJpaEntity() {}

  static ConsentLogJpaEntity from(ConsentChange change) {
    ConsentLogJpaEntity e = new ConsentLogJpaEntity();
    e.id = UUID.randomUUID();
    e.assignBrand(change.brandId());
    e.consumerId = change.consumerId().value();
    e.channel = change.channel();
    e.granted = change.granted();
    e.source = change.source();
    e.actor = change.actor();
    e.occurredAt = change.occurredAt();
    return e;
  }

  ConsentChange toDomain() {
    return new ConsentChange(
        getBrandId(), new ConsumerId(consumerId), channel, granted, source, actor, occurredAt);
  }
}
