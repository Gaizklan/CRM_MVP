package org.example.crm.consumer.domain;

import java.time.Instant;
import java.util.Objects;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;

/**
 * Registro auditable (inmutable) de un cambio de consentimiento: quién, cuándo, canal, valor y
 * origen.
 */
public record ConsentChange(
    BrandId brandId,
    ConsumerId consumerId,
    ConsentChannel channel,
    boolean granted,
    ConsentSource source,
    String actor,
    Instant occurredAt) {

  public ConsentChange {
    Objects.requireNonNull(brandId, "brandId");
    Objects.requireNonNull(consumerId, "consumerId");
    Objects.requireNonNull(channel, "channel");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(occurredAt, "occurredAt");
    if (actor == null || actor.isBlank() || actor.length() > 128) {
      throw new DomainException("Se requiere el actor del cambio de consentimiento (máx. 128)");
    }
  }
}
