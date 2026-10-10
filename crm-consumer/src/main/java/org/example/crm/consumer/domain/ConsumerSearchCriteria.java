package org.example.crm.consumer.domain;

import java.time.Instant;

/**
 * Filtros de la consulta administrativa de consumidores (todos opcionales). El email se filtra por
 * coincidencia exacta (normalizada) para ser compatible con el cifrado de PII.
 */
public record ConsumerSearchCriteria(
    Email email,
    ConsumerStatus status,
    LanguageTag language,
    String segment,
    Instant registeredFrom,
    Instant registeredTo) {

  public static ConsumerSearchCriteria all() {
    return new ConsumerSearchCriteria(null, null, null, null, null, null);
  }
}
