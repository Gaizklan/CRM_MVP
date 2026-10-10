package org.example.crm.common.application;

import java.util.UUID;
import org.example.crm.common.domain.DomainEvent;

/**
 * Consumidor fiable de eventos de dominio, alimentado desde el outbox transaccional: entrega
 * asíncrona <em>al menos una vez</em>, en la Marca del evento. Las implementaciones deben ser
 * idempotentes (usar {@code eventId} para deduplicar).
 *
 * <p>Para reacciones síncronas dentro de la misma transacción basta un {@code @EventListener} de
 * Spring; este contrato es para efectos que deben producirse aunque la aplicación caiga.
 */
public interface DomainEventHandler<E extends DomainEvent> {

  Class<E> eventType();

  void handle(E event, UUID eventId);
}
