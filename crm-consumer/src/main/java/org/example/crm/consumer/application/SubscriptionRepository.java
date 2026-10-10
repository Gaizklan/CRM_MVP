package org.example.crm.consumer.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;

/** Puerto de salida: catálogo de servicios y suscripciones (Marca del contexto). */
public interface SubscriptionRepository {

  List<SubscriptionService> findServices();

  Optional<SubscriptionService> findService(SubscriptionServiceId id);

  Optional<SubscriptionService> findServiceByCode(String code);

  SubscriptionService saveService(SubscriptionService service);

  Optional<Subscription> findActive(ConsumerId consumerId, SubscriptionServiceId serviceId);

  /** Suscripciones (activas e históricas) del consumidor, más reciente primero. */
  List<Subscription> findByConsumer(ConsumerId consumerId);

  /** Códigos de los servicios con suscripción activa del consumidor. */
  Set<String> activeServiceCodes(ConsumerId consumerId);

  Subscription save(Subscription subscription);
}
