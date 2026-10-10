package org.example.crm.consumer.domain;

import java.util.Locale;
import java.util.regex.Pattern;
import org.example.crm.common.domain.DomainException;

/** Dirección de email normalizada (sin espacios, en minúsculas). */
public record Email(String value) {

  private static final Pattern FORMAT =
      Pattern.compile(
          "[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+");

  public Email {
    if (value == null) {
      throw new DomainException("El email es obligatorio");
    }
    value = value.strip().toLowerCase(Locale.ROOT);
    if (value.length() > 254 || !FORMAT.matcher(value).matches()) {
      throw new DomainException("Email no válido: " + value);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
