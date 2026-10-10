package org.example.crm.admin.domain;

import org.example.crm.common.domain.DomainException;

/** Política mínima de contraseñas de usuarios internos. */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 12;
  public static final int MAX_LENGTH = 128;

  private PasswordPolicy() {}

  public static void validate(String rawPassword) {
    if (rawPassword == null
        || rawPassword.length() < MIN_LENGTH
        || rawPassword.length() > MAX_LENGTH
        || rawPassword.isBlank()) {
      throw new DomainException(
          "La contraseña debe tener entre %d y %d caracteres".formatted(MIN_LENGTH, MAX_LENGTH));
    }
  }
}
