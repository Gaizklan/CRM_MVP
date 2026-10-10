package org.example.crm.consumer.domain;

import java.time.Instant;
import java.util.Objects;

/** Estado vigente del consentimiento de un canal. */
public record ConsentStatus(boolean granted, ConsentSource source, Instant since) {

  public ConsentStatus {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(since, "since");
  }
}
