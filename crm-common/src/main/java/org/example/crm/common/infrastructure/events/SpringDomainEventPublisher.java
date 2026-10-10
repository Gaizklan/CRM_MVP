package org.example.crm.common.infrastructure.events;

import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.common.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publica los eventos de dominio como eventos de aplicación de Spring. */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher publisher;

  SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(DomainEvent event) {
    publisher.publishEvent(event);
  }
}
