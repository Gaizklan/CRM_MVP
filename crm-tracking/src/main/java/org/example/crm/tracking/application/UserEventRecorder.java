package org.example.crm.tracking.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.rules.application.UserEventLookup;
import org.example.crm.tracking.domain.UserEvent;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;

/**
 * Puerto de entrada del registro de eventos de usuario: lo usan el portal (login, logout, clics…),
 * la mensajería (email enviado/abierto/clic) y los handlers de eventos de dominio. Implementa
 * además {@link UserEventLookup} para el criterio {@code eventOccurred} de las reglas.
 */
public class UserEventRecorder implements UserEventLookup {

  private final UserEventRepository repository;
  private final Clock clock;

  public UserEventRecorder(UserEventRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Registra un evento de usuario en la Marca actual. */
  public UserEvent record(
      ConsumerId consumerId,
      UserEventType type,
      Instant occurredAt,
      Map<String, String> attributes) {
    UserEvent event =
        UserEvent.of(
            TenantContext.require(),
            consumerId,
            type,
            occurredAt == null ? clock.instant() : occurredAt,
            attributes);
    repository.save(event);
    return event;
  }

  /** Registra un evento derivado de un evento de dominio; idempotente por {@code sourceEventId}. */
  public void recordFromDomainEvent(
      UUID sourceEventId,
      ConsumerId consumerId,
      UserEventType type,
      Instant occurredAt,
      Map<String, String> attributes) {
    repository.save(
        new UserEvent(
            UUID.randomUUID(),
            TenantContext.require(),
            consumerId,
            type,
            occurredAt,
            attributes,
            sourceEventId));
  }

  public PageResult<UserEvent> search(UserEventCriteria criteria, PageQuery page) {
    TenantContext.require();
    return repository.search(criteria, page);
  }

  @Override
  public boolean occurredSince(ConsumerId consumer, String type, Instant since) {
    if (TenantContext.current().isEmpty()) {
      return false;
    }
    return UserEventType.parse(type)
        .map(t -> repository.existsSince(consumer, t, since))
        .orElse(false);
  }
}
