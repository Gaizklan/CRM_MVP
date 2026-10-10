package org.example.crm.common.domain;

/** El recurso solicitado no existe (o no es visible en el contexto de Marca actual). */
public class NotFoundException extends DomainException {

  public NotFoundException(String message) {
    super(message);
  }
}
