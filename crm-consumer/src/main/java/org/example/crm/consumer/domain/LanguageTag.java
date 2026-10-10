package org.example.crm.consumer.domain;

import java.util.regex.Pattern;
import org.example.crm.common.domain.DomainException;

/** Idioma BCP-47 simplificado: {@code es}, {@code es-ES}. */
public record LanguageTag(String value) {

  private static final Pattern FORMAT = Pattern.compile("[a-z]{2,3}(-[A-Z]{2})?");

  public LanguageTag {
    if (value == null || !FORMAT.matcher(value).matches()) {
      throw new DomainException("Idioma no válido (BCP-47, p. ej. 'es' o 'es-ES'): " + value);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
