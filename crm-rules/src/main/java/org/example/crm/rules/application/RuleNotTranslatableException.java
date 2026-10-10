package org.example.crm.rules.application;

import java.util.List;
import org.example.crm.common.domain.DomainException;

/** La regla es válida pero no puede traducirse a SQL (solo es evaluable en memoria). */
public class RuleNotTranslatableException extends DomainException {

  private final List<String> reasons;

  public RuleNotTranslatableException(List<String> reasons) {
    super("La regla no es traducible a SQL: " + String.join("; ", reasons));
    this.reasons = List.copyOf(reasons);
  }

  public List<String> reasons() {
    return reasons;
  }
}
