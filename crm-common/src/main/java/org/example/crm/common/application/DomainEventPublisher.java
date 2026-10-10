package org.example.crm.common.application;

import java.util.Collection;
import org.example.crm.common.domain.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio a otros módulos. */
public interface DomainEventPublisher {

  void publish(DomainEvent event);

  default void publishAll(Collection<? extends DomainEvent> events) {
    events.forEach(this::publish);
  }
}
