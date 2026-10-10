package org.example.crm.tracking.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.consumer.domain.ConsumerId;

/**
 * Evento de usuario registrado. {@code attributes} contiene datos de contexto (p. ej. {@code
 * campaignId}, {@code zone}); {@code sourceEventId} identifica el evento de dominio de origen
 * cuando lo hay, para registrar cada uno una sola vez.
 */
public record UserEvent(
    UUID id,
    BrandId brandId,
    ConsumerId consumerId,
    UserEventType type,
    Instant occurredAt,
    Map<String, String> attributes,
    UUID sourceEventId) {

  public static final int MAX_ATTRIBUTES = 20;
  private static final Pattern KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");

  public UserEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(brandId, "brandId");
    Objects.requireNonNull(consumerId, "consumerId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    if (attributes.size() > MAX_ATTRIBUTES) {
      throw new DomainException("Un evento admite como máximo " + MAX_ATTRIBUTES + " atributos");
    }
    attributes.forEach(
        (k, v) -> {
          if (!KEY.matcher(k).matches() || v == null || v.length() > 500) {
            throw new DomainException("Atributo de evento no válido: " + k);
          }
        });
  }

  public static UserEvent of(
      BrandId brandId,
      ConsumerId consumerId,
      UserEventType type,
      Instant occurredAt,
      Map<String, String> attributes) {
    return new UserEvent(
        UUID.randomUUID(), brandId, consumerId, type, occurredAt, attributes, null);
  }

  public Optional<UUID> source() {
    return Optional.ofNullable(sourceEventId);
  }
}
