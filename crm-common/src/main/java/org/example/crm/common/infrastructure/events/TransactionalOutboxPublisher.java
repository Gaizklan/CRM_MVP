package org.example.crm.common.infrastructure.events;

import java.sql.Timestamp;
import java.util.UUID;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.common.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publicación de eventos de dominio:
 *
 * <ol>
 *   <li>Inserta el evento en {@code domain_event_outbox} <b>en la misma transacción</b> que los
 *       datos de negocio (si la transacción se deshace, el evento no existe).
 *   <li>Lo publica como evento de Spring para los {@code @EventListener} síncronos.
 * </ol>
 *
 * {@link OutboxRelay} entrega después los eventos del outbox a los {@code DomainEventHandler}.
 */
public class TransactionalOutboxPublisher implements DomainEventPublisher {

  private final JdbcTemplate jdbc;
  private final OutboxEventSerializer serializer;
  private final ApplicationEventPublisher spring;

  public TransactionalOutboxPublisher(
      JdbcTemplate jdbc, OutboxEventSerializer serializer, ApplicationEventPublisher spring) {
    this.jdbc = jdbc;
    this.serializer = serializer;
    this.spring = spring;
  }

  @Override
  public void publish(DomainEvent event) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "Los eventos de dominio deben publicarse dentro de una transacción (outbox)");
    }
    jdbc.update(
        "INSERT INTO domain_event_outbox (id, brand_id, event_type, payload, occurred_at)"
            + " VALUES (?, ?, ?, ?::jsonb, ?)",
        UUID.randomUUID(),
        event.brandId().value(),
        serializer.typeOf(event),
        serializer.serialize(event),
        Timestamp.from(event.occurredAt()));
    spring.publishEvent(event);
  }
}
