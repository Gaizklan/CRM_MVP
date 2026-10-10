package org.example.crm.consumer.application;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso del catálogo de servicios y de las suscripciones de consumidores. */
@Transactional
public class SubscriptionManager implements SubscriptionQuery {

  private final SubscriptionRepository repository;
  private final ConsumerService consumers;
  private final ConsentChecker consents;
  private final Clock clock;

  public SubscriptionManager(
      SubscriptionRepository repository,
      ConsumerService consumers,
      ConsentChecker consents,
      Clock clock) {
    this.repository = repository;
    this.consumers = consumers;
    this.consents = consents;
    this.clock = clock;
  }

  // ---- Catálogo ----

  @Transactional(readOnly = true)
  public List<SubscriptionService> services() {
    TenantContext.require();
    return repository.findServices();
  }

  public SubscriptionService createService(String code, String name, String description) {
    var brand = TenantContext.require();
    if (repository.findServiceByCode(code).isPresent()) {
      throw new ConflictException("Ya existe el servicio " + code + " en la Marca");
    }
    return repository.saveService(SubscriptionService.create(brand, code, name, description));
  }

  public SubscriptionService updateService(
      SubscriptionServiceId id, String name, String description, boolean active) {
    TenantContext.require();
    SubscriptionService service =
        repository
            .findService(id)
            .orElseThrow(() -> new NotFoundException("Servicio no encontrado: " + id));
    service.update(name, description, active);
    return repository.saveService(service);
  }

  // ---- Suscripciones ----

  /** Suscribe al consumidor; requiere opt-in vigente de servicios de suscripción. */
  public Subscription subscribe(ConsumerId consumerId, String serviceCode) {
    var consumer = consumers.get(consumerId);
    SubscriptionService service = serviceByCode(serviceCode);
    if (!consents.isAllowed(consumerId, ConsentChannel.SUBSCRIPTIONS)) {
      throw new DomainException(
          "El consumidor no tiene opt-in de servicios de suscripción (SUBSCRIPTIONS)");
    }
    if (repository.findActive(consumerId, service.id()).isPresent()) {
      throw new ConflictException("El consumidor ya está suscrito a " + serviceCode);
    }
    return repository.save(Subscription.start(consumerId, consumer.brandId(), service, clock));
  }

  /** Da de baja la suscripción activa (idempotente: sin suscripción activa no hace nada). */
  public void unsubscribe(ConsumerId consumerId, String serviceCode) {
    consumers.get(consumerId);
    SubscriptionService service = serviceByCode(serviceCode);
    repository
        .findActive(consumerId, service.id())
        .ifPresent(
            s -> {
              s.cancel(clock);
              repository.save(s);
            });
  }

  @Transactional(readOnly = true)
  public List<Subscription> subscriptions(ConsumerId consumerId) {
    consumers.get(consumerId);
    return repository.findByConsumer(consumerId);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> activeServiceCodes(ConsumerId consumerId) {
    if (TenantContext.current().isEmpty()) {
      return Set.of();
    }
    return repository.activeServiceCodes(consumerId);
  }

  private SubscriptionService serviceByCode(String code) {
    return repository
        .findServiceByCode(code)
        .orElseThrow(() -> new NotFoundException("Servicio no encontrado: " + code));
  }
}
