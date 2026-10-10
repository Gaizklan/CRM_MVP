package org.example.crm.common.application;

import org.example.crm.common.domain.DomainException;

/** Petición de página (base 0). */
public record PageQuery(int page, int size) {

  public static final int MAX_SIZE = 200;

  public PageQuery {
    if (page < 0) {
      throw new DomainException("La página debe ser >= 0");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new DomainException("El tamaño de página debe estar entre 1 y " + MAX_SIZE);
    }
  }
}
