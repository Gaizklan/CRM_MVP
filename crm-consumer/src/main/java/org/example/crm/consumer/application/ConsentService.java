package org.example.crm.consumer.application;

import java.time.Clock;
import java.util.List;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.common.domain.DomainException;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consents;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerStatus;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de consentimientos y verificación de opt-in. */
@Transactional
public class ConsentService implements ConsentChecker {

  private final ConsumerService consumers;
  private final ConsentRepository repository;
  private final DomainEventPublisher events;
  private final Clock clock;

  public ConsentService(
      ConsumerService consumers,
      ConsentRepository repository,
      DomainEventPublisher events,
      Clock clock) {
    this.consumers = consumers;
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  /** Registra un cambio de consentimiento con su origen y actor. */
  public Consents change(
      ConsumerId consumerId,
      ConsentChannel channel,
      boolean granted,
      ConsentSource source,
      String actor) {
    Consumer consumer = consumers.get(consumerId);
    if (granted && consumer.status() == ConsumerStatus.WITHDRAWN) {
      throw new DomainException("No se puede conceder consentimiento a un consumidor dado de baja");
    }
    Consents consents = repository.load(consumerId);
    consents.change(channel, granted, source, actor, clock);
    List<ConsentChange> changes = consents.pullChanges();
    var pending = consents.pullEvents();
    if (!changes.isEmpty()) {
      repository.save(consents, changes);
      events.publishAll(pending);
    }
    return consents;
  }

  @Transactional(readOnly = true)
  public Consents current(ConsumerId consumerId) {
    consumers.get(consumerId);
    return repository.load(consumerId);
  }

  @Transactional(readOnly = true)
  public List<ConsentChange> history(ConsumerId consumerId) {
    consumers.get(consumerId);
    return repository.history(consumerId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isAllowed(ConsumerId consumerId, ConsentChannel channel) {
    return consumers
        .find(consumerId)
        .filter(c -> c.status() != ConsumerStatus.WITHDRAWN)
        .map(c -> repository.load(consumerId).isAllowed(channel))
        .orElse(false);
  }
}
