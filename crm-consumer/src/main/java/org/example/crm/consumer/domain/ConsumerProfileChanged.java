package org.example.crm.consumer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;

/**
 * El perfil o las preferencias de un consumidor han cambiado. {@code changedFields} usa los nombres
 * de campo del DSL de reglas: {@code email}, {@code phone}, {@code language}, {@code segment},
 * {@code status}, {@code attribute.<nombre>} y {@code preference.<clave>}.
 */
public record ConsumerProfileChanged(
    BrandId brandId, ConsumerId consumerId, Set<String> changedFields, Instant occurredAt)
    implements DomainEvent {

  public ConsumerProfileChanged {
    Objects.requireNonNull(brandId, "brandId");
    Objects.requireNonNull(consumerId, "consumerId");
    Objects.requireNonNull(occurredAt, "occurredAt");
    changedFields = Set.copyOf(changedFields);
  }
}
