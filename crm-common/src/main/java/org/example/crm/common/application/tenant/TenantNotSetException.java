package org.example.crm.common.application.tenant;

import org.example.crm.common.domain.DomainException;

/** Se requiere un contexto de Marca y no se ha resuelto ninguno. */
public class TenantNotSetException extends DomainException {

  public TenantNotSetException() {
    super("Operación que requiere una Marca: indique la Marca (cabecera X-Brand o selector)");
  }
}
