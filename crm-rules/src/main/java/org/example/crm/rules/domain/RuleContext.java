package org.example.crm.rules.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.PreferenceValue;

/**
 * Datos de un consumidor contra los que se evalúa una regla en memoria.
 *
 * @param profile campos de perfil ({@code email, phone, language, segment, status} como texto;
 *     {@code registeredAt} como {@link Instant}); un campo ausente no está en el mapa
 * @param attributes atributos de perfil
 * @param preferences preferencias tipadas
 * @param allowedChannels canales permitidos (opt-in del canal y del programa)
 * @param subscriptions códigos de servicios con suscripción activa
 * @param events consulta de eventos de usuario recientes
 * @param now instante de evaluación (para ventanas temporales)
 */
public record RuleContext(
    ConsumerId consumerId,
    Map<String, Object> profile,
    Map<String, String> attributes,
    Map<String, PreferenceValue> preferences,
    Set<ConsentChannel> allowedChannels,
    Set<String> subscriptions,
    RecentEvents events,
    Instant now) {

  public RuleContext {
    Objects.requireNonNull(consumerId, "consumerId");
    profile = Map.copyOf(profile);
    attributes = Map.copyOf(attributes);
    preferences = Map.copyOf(preferences);
    allowedChannels = Set.copyOf(allowedChannels);
    subscriptions = Set.copyOf(subscriptions);
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(now, "now");
  }

  /** Consulta de eventos de usuario del consumidor. */
  @FunctionalInterface
  public interface RecentEvents {

    /** {@code true} si hay al menos un evento del tipo con fecha en {@code [since, now]}. */
    boolean occurredSince(String type, Instant since);

    static RecentEvents none() {
      return (type, since) -> false;
    }
  }

  /** {@code true} si hay un evento del tipo en la ventana {@code within} hasta {@code now}. */
  public boolean eventOccurred(String type, Duration within) {
    return events.occurredSince(type, now.minus(within));
  }
}
