package org.example.crm.consumer.domain;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionServiceId(UUID value) {

  public SubscriptionServiceId {
    Objects.requireNonNull(value, "SubscriptionServiceId requiere un valor");
  }

  public static SubscriptionServiceId random() {
    return new SubscriptionServiceId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
