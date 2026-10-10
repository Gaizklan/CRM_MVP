package org.example.crm.rules.domain;

/** Tipo estático de un campo del DSL. */
public enum ValueType {
  STRING,
  /** Instante (ISO-8601) o fecha (yyyy-MM-dd, inicio del día UTC). */
  DATETIME,
  BOOLEAN,
  /** Conjunto de textos (p. ej. códigos de servicios suscritos). */
  SET,
  /** Tipo conocido solo en tiempo de evaluación (preferencias: texto, número, booleano o fecha). */
  DYNAMIC
}
