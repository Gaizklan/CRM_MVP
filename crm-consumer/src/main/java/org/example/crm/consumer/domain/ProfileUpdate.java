package org.example.crm.consumer.domain;

import java.util.Map;
import java.util.Optional;

/**
 * Cambio parcial de perfil: un campo {@code null} no se modifica. Para {@code phone} y {@code
 * segment}, {@code Optional.empty()} los borra. En {@code attributes}, un valor {@code null}
 * elimina el atributo.
 */
public record ProfileUpdate(
    Email email,
    Optional<PhoneNumber> phone,
    LanguageTag language,
    Optional<String> segment,
    Map<String, String> attributes) {

  public static ProfileUpdate none() {
    return new ProfileUpdate(null, null, null, null, null);
  }
}
