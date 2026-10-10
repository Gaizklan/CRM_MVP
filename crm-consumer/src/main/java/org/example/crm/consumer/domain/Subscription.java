package org.example.crm.consumer.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;

/**
 * Suscripción de un consumidor a un servicio. Una baja cierra la suscripción ({@code
 * unsubscribedAt}); volver a suscribirse crea una nueva, de modo que se conserva el histórico.
 */
public final class Subscription {

  private final UUID id;
  private final BrandId brandId;
  private final ConsumerId consumerId;
  private final SubscriptionServiceId serviceId;
  private final Instant subscribedAt;
  private Instant unsubscribedAt;

  private Subscription(
      UUID id,
      BrandId brandId,
      ConsumerId consumerId,
      SubscriptionServiceId serviceId,
      Instant subscribedAt,
      Instant unsubscribedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.brandId = Objects.requireNonNull(brandId, "brandId");
    this.consumerId = Objects.requireNonNull(consumerId, "consumerId");
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
    this.subscribedAt = Objects.requireNonNull(subscribedAt, "subscribedAt");
    this.unsubscribedAt = unsubscribedAt;
  }

  /** Alta en un servicio. El servicio debe pertenecer a la misma Marca y estar activo. */
  public static Subscription start(
      ConsumerId consumerId, BrandId brandId, SubscriptionService service, Clock clock) {
    if (!service.brandId().equals(brandId)) {
      throw new DomainException("El servicio no pertenece a la Marca del consumidor");
    }
    if (!service.active()) {
      throw new DomainException("El servicio " + service.code() + " no está activo");
    }
    return new Subscription(
        UUID.randomUUID(), brandId, consumerId, service.id(), clock.instant(), null);
  }

  public static Subscription restore(
      UUID id,
      BrandId brandId,
      ConsumerId consumerId,
      SubscriptionServiceId serviceId,
      Instant subscribedAt,
      Instant unsubscribedAt) {
    return new Subscription(id, brandId, consumerId, serviceId, subscribedAt, unsubscribedAt);
  }

  /** Baja. Idempotente: si ya está dada de baja, no cambia. */
  public void cancel(Clock clock) {
    if (unsubscribedAt == null) {
      unsubscribedAt = clock.instant();
    }
  }

  public boolean isActive() {
    return unsubscribedAt == null;
  }

  public UUID id() {
    return id;
  }

  public BrandId brandId() {
    return brandId;
  }

  public ConsumerId consumerId() {
    return consumerId;
  }

  public SubscriptionServiceId serviceId() {
    return serviceId;
  }

  public Instant subscribedAt() {
    return subscribedAt;
  }

  public Optional<Instant> unsubscribedAt() {
    return Optional.ofNullable(unsubscribedAt);
  }
}
