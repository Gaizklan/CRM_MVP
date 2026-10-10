package org.example.crm.consumer.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.example.crm.common.domain.DomainException;

/** Valor tipado de una preferencia. */
public sealed interface PreferenceValue {

  PreferenceType type();

  /** Valor Java nativo ({@link String}, {@link BigDecimal}, {@link Boolean}, {@link LocalDate}). */
  Object raw();

  /** Representación textual canónica (para historial y API). */
  String asText();

  static PreferenceValue of(String text) {
    return new StringValue(text);
  }

  static PreferenceValue of(BigDecimal number) {
    return new NumberValue(number);
  }

  static PreferenceValue of(boolean flag) {
    return new BooleanValue(flag);
  }

  static PreferenceValue of(LocalDate date) {
    return new DateValue(date);
  }

  /** Interpreta {@code text} según {@code type}. */
  static PreferenceValue parse(PreferenceType type, String text) {
    Objects.requireNonNull(type, "type");
    if (text == null) {
      throw new DomainException("El valor de la preferencia es obligatorio");
    }
    try {
      return switch (type) {
        case STRING -> new StringValue(text);
        case NUMBER -> new NumberValue(new BigDecimal(text.strip()));
        case BOOLEAN -> new BooleanValue(parseBoolean(text));
        case DATE -> new DateValue(LocalDate.parse(text.strip()));
      };
    } catch (NumberFormatException | DateTimeParseException e) {
      throw new DomainException("Valor '%s' no válido para el tipo %s".formatted(text, type));
    }
  }

  private static boolean parseBoolean(String text) {
    return switch (text.strip().toLowerCase(java.util.Locale.ROOT)) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new DomainException("Valor booleano no válido: " + text);
    };
  }

  record StringValue(String value) implements PreferenceValue {
    public StringValue {
      if (value == null || value.length() > 1000) {
        throw new DomainException("Texto de preferencia obligatorio (máx. 1000 caracteres)");
      }
    }

    @Override
    public PreferenceType type() {
      return PreferenceType.STRING;
    }

    @Override
    public Object raw() {
      return value;
    }

    @Override
    public String asText() {
      return value;
    }
  }

  record NumberValue(BigDecimal value) implements PreferenceValue {
    public NumberValue {
      Objects.requireNonNull(value, "value");
      value = value.stripTrailingZeros();
    }

    @Override
    public PreferenceType type() {
      return PreferenceType.NUMBER;
    }

    @Override
    public Object raw() {
      return value;
    }

    @Override
    public String asText() {
      return value.toPlainString();
    }
  }

  record BooleanValue(boolean value) implements PreferenceValue {
    @Override
    public PreferenceType type() {
      return PreferenceType.BOOLEAN;
    }

    @Override
    public Object raw() {
      return value;
    }

    @Override
    public String asText() {
      return Boolean.toString(value);
    }
  }

  record DateValue(LocalDate value) implements PreferenceValue {
    public DateValue {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public PreferenceType type() {
      return PreferenceType.DATE;
    }

    @Override
    public Object raw() {
      return value;
    }

    @Override
    public String asText() {
      return value.toString();
    }
  }
}
