package org.example.crm.common.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una Marca. Toda entidad de negocio lo lleva. */
public record BrandId(UUID value) {

  public BrandId {
    Objects.requireNonNull(value, "BrandId requiere un valor");
  }

  public static BrandId of(String value) {
    try {
      return new BrandId(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      throw new DomainException("Identificador de Marca no válido: " + value);
    }
  }

  public static BrandId random() {
    return new BrandId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
