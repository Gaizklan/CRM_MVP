package org.example.crm.rules.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Árbol de una regla de negocio. Se construye con {@link RuleParser} (que valida) y se evalúa con
 * {@link RuleEvaluator}.
 *
 * <p>Semántica de valores ausentes (lógica de dos valores): toda comparación sobre un campo sin
 * valor es falsa, salvo {@code ne}, que se define como {@code not eq} y por tanto es verdadera.
 */
public sealed interface Rule {

  record And(List<Rule> rules) implements Rule {
    public And {
      rules = List.copyOf(rules);
    }
  }

  record Or(List<Rule> rules) implements Rule {
    public Or {
      rules = List.copyOf(rules);
    }
  }

  record Not(Rule rule) implements Rule {
    public Not {
      Objects.requireNonNull(rule, "rule");
    }
  }

  /** Comparación binaria: {@code eq, ne, gt, lt}. */
  record Compare(Op op, FieldRef field, Literal value) implements Rule {
    public Compare {
      Objects.requireNonNull(op, "op");
      Objects.requireNonNull(field, "field");
      Objects.requireNonNull(value, "value");
    }
  }

  record In(FieldRef field, List<Literal> values) implements Rule {
    public In {
      Objects.requireNonNull(field, "field");
      values = List.copyOf(values);
    }
  }

  /** Rango cerrado {@code [from, to]}. */
  record Between(FieldRef field, Literal from, Literal to) implements Rule {
    public Between {
      Objects.requireNonNull(field, "field");
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
    }
  }

  record Exists(FieldRef field) implements Rule {
    public Exists {
      Objects.requireNonNull(field, "field");
    }
  }

  /** Texto que contiene la subcadena, o conjunto que contiene el elemento. */
  record Contains(FieldRef field, Literal value) implements Rule {
    public Contains {
      Objects.requireNonNull(field, "field");
      Objects.requireNonNull(value, "value");
    }
  }

  /** El consumidor tiene una suscripción activa al servicio con este código. */
  record SubscribedTo(String serviceCode) implements Rule {
    public SubscribedTo {
      Objects.requireNonNull(serviceCode, "serviceCode");
    }
  }

  /** Ha ocurrido un evento de usuario del tipo indicado en la ventana {@code within}. */
  record EventOccurred(String type, Duration within) implements Rule {
    public EventOccurred {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(within, "within");
    }
  }

  /** Operadores de comparación binaria. */
  enum Op {
    EQ("eq"),
    NE("ne"),
    GT("gt"),
    LT("lt");

    private final String json;

    Op(String json) {
      this.json = json;
    }

    public String json() {
      return json;
    }
  }
}
