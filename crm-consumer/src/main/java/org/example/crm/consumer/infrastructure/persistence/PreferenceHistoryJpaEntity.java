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
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.PreferenceChange;
import org.example.crm.consumer.domain.PreferenceType;
import org.example.crm.consumer.domain.PreferenceValue;

/** Historial (solo inserción) de cambios de preferencias. */
@Entity
@Table(name = "consumer_preference_history")
public class PreferenceHistoryJpaEntity extends BrandScopedEntity {

  @Id private UUID id;

  @Column(name = "consumer_id", nullable = false, updatable = false)
  private UUID consumerId;

  @Column(name = "pref_key", nullable = false, updatable = false, length = 64)
  private String key;

  @Enumerated(EnumType.STRING)
  @Column(name = "old_type", updatable = false, length = 16)
  private PreferenceType oldType;

  @Column(name = "old_value", updatable = false, length = 1000)
  private String oldValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_type", updatable = false, length = 16)
  private PreferenceType newType;

  @Column(name = "new_value", updatable = false, length = 1000)
  private String newValue;

  @Column(name = "changed_by", nullable = false, updatable = false, length = 128)
  private String changedBy;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected PreferenceHistoryJpaEntity() {}

  static PreferenceHistoryJpaEntity from(PreferenceChange change) {
    PreferenceHistoryJpaEntity e = new PreferenceHistoryJpaEntity();
    e.id = UUID.randomUUID();
    e.assignBrand(change.brandId());
    e.consumerId = change.consumerId().value();
    e.key = change.key();
    change
        .previous()
        .ifPresent(
            v -> {
              e.oldType = v.type();
              e.oldValue = v.asText();
            });
    change
        .current()
        .ifPresent(
            v -> {
              e.newType = v.type();
              e.newValue = v.asText();
            });
    e.changedBy = change.changedBy();
    e.changedAt = change.changedAt();
    return e;
  }

  PreferenceChange toDomain() {
    return new PreferenceChange(
        getBrandId(),
        new ConsumerId(consumerId),
        key,
        oldType == null ? null : PreferenceValue.parse(oldType, oldValue),
        newType == null ? null : PreferenceValue.parse(newType, newValue),
        changedBy,
        changedAt);
  }
}
