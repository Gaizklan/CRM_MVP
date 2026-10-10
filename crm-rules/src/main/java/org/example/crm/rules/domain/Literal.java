package org.example.crm.rules.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Valor literal de una regla: texto, número o booleano. Las fechas se escriben como texto ISO. */
public sealed interface Literal {

  /** Valor Java para serializar ({@link String}, {@link BigDecimal} o {@link Boolean}). */
  Object raw();

  record Text(String value) implements Literal {
    public Text {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public Object raw() {
      return value;
    }
  }

  record Number(BigDecimal value) implements Literal {
    public Number {
      Objects.requireNonNull(value, "value");
      value = value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    @Override
    public Object raw() {
      return value;
    }
  }

  record Bool(boolean value) implements Literal {
    @Override
    public Object raw() {
      return value;
    }
  }
}
