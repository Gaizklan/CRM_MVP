package org.example.crm.common.domain;

/** La operación entra en conflicto con el estado actual (p. ej. un valor único ya existe). */
public class ConflictException extends DomainException {

  public ConflictException(String message) {
    super(message);
  }
}
