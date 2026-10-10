package org.example.crm.consumer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.example.crm.common.domain.BrandId;

/**
 * Entrada del historial de preferencias: valor anterior y nuevo (vacío si se crea o se elimina).
 */
public record PreferenceChange(
    BrandId brandId,
    ConsumerId consumerId,
    String key,
    PreferenceValue oldValue,
    PreferenceValue newValue,
    String changedBy,
    Instant changedAt) {

  public PreferenceChange {
    Objects.requireNonNull(brandId, "brandId");
    Objects.requireNonNull(consumerId, "consumerId");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(changedBy, "changedBy");
    Objects.requireNonNull(changedAt, "changedAt");
  }

  public Optional<PreferenceValue> previous() {
    return Optional.ofNullable(oldValue);
  }

  public Optional<PreferenceValue> current() {
    return Optional.ofNullable(newValue);
  }
}
