package org.example.crm.consumer.domain;

import java.util.Objects;
import java.util.UUID;
import org.example.crm.common.domain.DomainException;

public record ConsumerId(UUID value) {

  public ConsumerId {
    Objects.requireNonNull(value, "ConsumerId requiere un valor");
  }

  public static ConsumerId random() {
    return new ConsumerId(UUID.randomUUID());
  }

  public static ConsumerId of(String value) {
    try {
      return new ConsumerId(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      throw new DomainException("Identificador de consumidor no válido: " + value);
    }
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
