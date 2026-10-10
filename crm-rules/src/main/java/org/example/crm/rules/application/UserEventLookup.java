package org.example.crm.rules.application;

import java.time.Instant;
import org.example.crm.consumer.domain.ConsumerId;

/**
 * Puerto de salida: consulta de eventos de usuario para el criterio {@code eventOccurred}. Lo
 * implementa el módulo de tracking (P11); mientras no exista, no hay eventos.
 */
public interface UserEventLookup {

  /** {@code true} si el consumidor tiene un evento del tipo con fecha {@code >= since}. */
  boolean occurredSince(ConsumerId consumer, String type, Instant since);
}
