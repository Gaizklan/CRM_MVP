package org.example.crm.consumer.domain;

/** Origen de un cambio de consentimiento. */
public enum ConsentSource {
  /** Portal web (el propio consumidor). */
  WEB,
  /** Enlace en una comunicación por email (p. ej. baja). */
  EMAIL,
  /** Usuario interno desde la administración. */
  ADMIN,
  /** Importación masiva. */
  IMPORT
}
