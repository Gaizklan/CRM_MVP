package org.example.crm.tracking.domain;

import java.time.Instant;
import org.example.crm.common.domain.DomainException;
import org.example.crm.consumer.domain.ConsumerId;

/**
 * Filtros de consulta de eventos (todos opcionales): consumidor, tipo y rango {@code [from, to)}.
 */
public record UserEventCriteria(
    ConsumerId consumerId, UserEventType type, Instant from, Instant to) {

  public UserEventCriteria {
    if (from != null && to != null && !from.isBefore(to)) {
      throw new DomainException("El rango de fechas debe cumplir from < to");
    }
  }
}
