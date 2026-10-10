package org.example.crm.common.domain;

/** Violación de una regla de negocio o dato de entrada no válido. */
public class DomainException extends RuntimeException {

  public DomainException(String message) {
    super(message);
  }
}
