package org.example.crm.admin.domain;

import java.util.Objects;
import java.util.UUID;

public record InternalUserId(UUID value) {

  public InternalUserId {
    Objects.requireNonNull(value, "InternalUserId requiere un valor");
  }

  public static InternalUserId random() {
    return new InternalUserId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
