package org.example.crm.consumer.domain;

import java.util.regex.Pattern;
import org.example.crm.common.domain.DomainException;

/** Teléfono en formato E.164 ({@code +<código país><número>}); se eliminan espacios y guiones. */
public record PhoneNumber(String value) {

  private static final Pattern E164 = Pattern.compile("\\+[1-9][0-9]{6,14}");

  public PhoneNumber {
    if (value == null) {
      throw new DomainException("El teléfono es obligatorio");
    }
    value = value.replaceAll("[\\s()-]", "");
    if (!E164.matcher(value).matches()) {
      throw new DomainException("Teléfono no válido (formato E.164, p. ej. +34600000000)");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
