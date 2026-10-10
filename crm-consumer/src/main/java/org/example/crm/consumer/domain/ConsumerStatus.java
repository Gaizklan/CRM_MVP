package org.example.crm.consumer.domain;

/** Estado del consumidor en el programa CRM de su Marca. */
public enum ConsumerStatus {
  /** Activo: puede navegar y recibir comunicaciones (según consentimientos). */
  ACTIVE,
  /** Bloqueado (p. ej. por intentos de login fallidos). */
  LOCKED,
  /** Dado de baja del programa. Estado final. */
  WITHDRAWN
}
