package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.example.crm.consumer.application.SubscriptionRepository;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaSubscriptionRepository implements SubscriptionRepository {

  private final SpringDataSubscriptionServiceRepository services;
  private final SpringDataSubscriptionRepository subscriptions;

  JpaSubscriptionRepository(
      SpringDataSubscriptionServiceRepository services,
      SpringDataSubscriptionRepository subscriptions) {
    this.services = services;
    this.subscriptions = subscriptions;
  }

  @Override
  public List<SubscriptionService> findServices() {
    return services.findAll(Sort.by("code")).stream()
        .map(SubscriptionServiceJpaEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<SubscriptionService> findService(SubscriptionServiceId id) {
    return services.findById(id.value()).map(SubscriptionServiceJpaEntity::toDomain);
  }

  @Override
  public Optional<SubscriptionService> findServiceByCode(String code) {
    return services.findByCode(code).map(SubscriptionServiceJpaEntity::toDomain);
  }

  @Override
  @Transactional
  public SubscriptionService saveService(SubscriptionService service) {
    SubscriptionServiceJpaEntity entity =
        services
            .findById(service.id().value())
            .map(
                e -> {
                  e.apply(service);
                  return e;
                })
            .orElseGet(() -> SubscriptionServiceJpaEntity.create(service));
    return services.saveAndFlush(entity).toDomain();
  }

  @Override
  public Optional<Subscription> findActive(ConsumerId consumerId, SubscriptionServiceId serviceId) {
    return subscriptions
        .findByConsumerIdAndServiceIdAndUnsubscribedAtIsNull(consumerId.value(), serviceId.value())
        .map(SubscriptionJpaEntity::toDomain);
  }

  @Override
  public List<Subscription> findByConsumer(ConsumerId consumerId) {
    return subscriptions.findByConsumerIdOrderBySubscribedAtDesc(consumerId.value()).stream()
        .map(SubscriptionJpaEntity::toDomain)
        .toList();
  }

  @Override
  public Set<String> activeServiceCodes(ConsumerId consumerId) {
    return new TreeSet<>(subscriptions.activeServiceCodes(consumerId.value()));
  }

  @Override
  @Transactional
  public Subscription save(Subscription subscription) {
    SubscriptionJpaEntity entity =
        subscriptions
            .findById(subscription.id())
            .map(
                e -> {
                  e.apply(subscription);
                  return e;
                })
            .orElseGet(() -> SubscriptionJpaEntity.create(subscription));
    return subscriptions.saveAndFlush(entity).toDomain();
  }
}
