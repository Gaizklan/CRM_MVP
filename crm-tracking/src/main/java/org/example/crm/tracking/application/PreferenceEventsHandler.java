package org.example.crm.tracking.application;

import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.DomainEventHandler;
import org.example.crm.consumer.domain.ConsentChanged;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.tracking.domain.UserEventType;

/**
 * Deriva eventos de usuario {@code PREFERENCES_CHANGED} de los eventos de dominio del módulo de
 * consumidores (cambios de preferencias y de consentimientos), entregados por el outbox.
 */
public final class PreferenceEventsHandler {

  private PreferenceEventsHandler() {}

  public static DomainEventHandler<ConsumerProfileChanged> profileChanges(
      UserEventRecorder recorder) {
    return new DomainEventHandler<>() {
      @Override
      public Class<ConsumerProfileChanged> eventType() {
        return ConsumerProfileChanged.class;
      }

      @Override
      public void handle(ConsumerProfileChanged event, UUID eventId) {
        String keys =
            String.join(
                ",",
                event.changedFields().stream()
                    .filter(f -> f.startsWith("preference."))
                    .map(f -> f.substring("preference.".length()))
                    .sorted()
                    .toList());
        if (!keys.isEmpty()) {
          recorder.recordFromDomainEvent(
              eventId,
              event.consumerId(),
              UserEventType.PREFERENCES_CHANGED,
              event.occurredAt(),
              Map.of("preferences", keys.length() > 500 ? keys.substring(0, 500) : keys));
        }
      }
    };
  }

  public static DomainEventHandler<ConsentChanged> consentChanges(UserEventRecorder recorder) {
    return new DomainEventHandler<>() {
      @Override
      public Class<ConsentChanged> eventType() {
        return ConsentChanged.class;
      }

      @Override
      public void handle(ConsentChanged event, UUID eventId) {
        recorder.recordFromDomainEvent(
            eventId,
            event.consumerId(),
            UserEventType.PREFERENCES_CHANGED,
            event.occurredAt(),
            Map.of(
                "consentChannel", event.channel().name(),
                "granted", Boolean.toString(event.granted()),
                "source", event.source().name()));
      }
    };
  }
}
