package org.example.crm.rules.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/** Interpretación de literales de fecha del DSL. */
final class Temporal {

  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  private Temporal() {}

  /** Fecha {@code yyyy-MM-dd}. */
  static Optional<LocalDate> date(String text) {
    if (text == null || !DATE.matcher(text).matches()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(text));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /** Instante ISO-8601 o fecha (inicio del día en UTC). */
  static Optional<Instant> instant(String text) {
    Optional<LocalDate> date = date(text);
    if (date.isPresent()) {
      return Optional.of(date.get().atStartOfDay(ZoneOffset.UTC).toInstant());
    }
    try {
      return Optional.of(Instant.parse(text));
    } catch (DateTimeParseException | NullPointerException e) {
      return Optional.empty();
    }
  }
}
