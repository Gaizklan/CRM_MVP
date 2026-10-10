package org.example.crm.common.domain;

import java.time.Instant;

/** Evento de dominio: algo relevante para el negocio que ya ha ocurrido. */
public interface DomainEvent {

  /** Marca a la que pertenece el evento. */
  BrandId brandId();

  /** Momento en que ocurrió. */
  Instant occurredAt();
}
