package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.crm.consumer.domain.PreferenceType;
import org.example.crm.consumer.domain.PreferenceValue;

/**
 * Preferencia con una columna por tipo, para que las reglas traducidas a SQL comparen con el tipo
 * nativo (número, fecha, booleano).
 */
@Embeddable
public class PreferenceEmbeddable {

  @Enumerated(EnumType.STRING)
  @Column(name = "pref_type", nullable = false, length = 16)
  private PreferenceType type;

  @Column(name = "string_value", length = 1000)
  private String stringValue;

  @Column(name = "number_value", precision = 38, scale = 10)
  private BigDecimal numberValue;

  @Column(name = "boolean_value")
  private Boolean booleanValue;

  @Column(name = "date_value")
  private LocalDate dateValue;

  protected PreferenceEmbeddable() {}

  static PreferenceEmbeddable from(PreferenceValue value) {
    PreferenceEmbeddable e = new PreferenceEmbeddable();
    e.type = value.type();
    switch (value) {
      case PreferenceValue.StringValue s -> e.stringValue = s.value();
      case PreferenceValue.NumberValue n -> e.numberValue = n.value();
      case PreferenceValue.BooleanValue b -> e.booleanValue = b.value();
      case PreferenceValue.DateValue d -> e.dateValue = d.value();
    }
    return e;
  }

  PreferenceValue toDomain() {
    return switch (type) {
      case STRING -> PreferenceValue.of(stringValue);
      case NUMBER -> PreferenceValue.of(numberValue);
      case BOOLEAN -> PreferenceValue.of(booleanValue);
      case DATE -> PreferenceValue.of(dateValue);
    };
  }
}
