package org.example.crm.rules.domain;

import java.util.List;
import org.example.crm.common.domain.DomainException;

/** La regla no es válida. {@link #errors()} contiene un mensaje por problema, con su ruta JSON. */
public class RuleValidationException extends DomainException {

  private final List<String> errors;

  public RuleValidationException(List<String> errors) {
    super("Regla no válida: " + String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> errors() {
    return errors;
  }
}
