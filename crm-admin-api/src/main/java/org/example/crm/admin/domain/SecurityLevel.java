package org.example.crm.admin.domain;

/** Nivel de seguridad de un usuario interno (requisitos §3.1). */
public enum SecurityLevel {
  /** Gestiona usuarios internos y Marcas; opera sobre todas las Marcas. */
  ADMIN,
  /** Opera sobre todas las Marcas. */
  GLOBAL_USER,
  /** Opera solo sobre su Marca. */
  LOCAL_USER
}
