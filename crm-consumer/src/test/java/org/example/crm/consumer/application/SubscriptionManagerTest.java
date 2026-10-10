package org.example.crm.consumer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionManagerTest {

  static final class InMemorySubscriptions implements SubscriptionRepository {
    final List<SubscriptionService> services = new ArrayList<>();
    final List<Subscription> subscriptions = new ArrayList<>();

    private boolean inBrand(BrandId b) {
      return TenantContext.current().map(b::equals).orElse(false);
    }

    @Override
    public List<SubscriptionService> findServices() {
      return services.stream().filter(s -> inBrand(s.brandId())).toList();
    }

    @Override
    public Optional<SubscriptionService> findService(SubscriptionServiceId id) {
      return findServices().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    @Override
    public Optional<SubscriptionService> findServiceByCode(String code) {
      return findServices().stream().filter(s -> s.code().equals(code)).findFirst();
    }

    @Override
    public SubscriptionService saveService(SubscriptionService service) {
      services.removeIf(s -> s.id().equals(service.id()));
      services.add(service);
      return service;
    }

    @Override
    public Optional<Subscription> findActive(
        ConsumerId consumerId, SubscriptionServiceId serviceId) {
      return subscriptions.stream()
          .filter(s -> s.consumerId().equals(consumerId) && s.serviceId().equals(serviceId))
          .filter(Subscription::isActive)
          .findFirst();
    }

    @Override
    public List<Subscription> findByConsumer(ConsumerId consumerId) {
      return subscriptions.stream()
          .filter(s -> s.consumerId().equals(consumerId))
          .sorted(Comparator.comparing(Subscription::subscribedAt).reversed())
          .toList();
    }

    @Override
    public Set<String> activeServiceCodes(ConsumerId consumerId) {
      Set<String> codes = new TreeSet<>();
      subscriptions.stream()
          .filter(s -> s.consumerId().equals(consumerId) && s.isActive() && inBrand(s.brandId()))
          .forEach(
              s ->
                  services.stream()
                      .filter(x -> x.id().equals(s.serviceId()))
                      .forEach(x -> codes.add(x.code())));
      return codes;
    }

    @Override
    public Subscription save(Subscription subscription) {
      subscriptions.removeIf(s -> s.id().equals(subscription.id()));
      subscriptions.add(subscription);
      return subscription;
    }
  }

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final ConsumerService consumers =
      new ConsumerService(new InMemoryConsumerRepository(), e -> {}, clock);
  private final ConsentService consents =
      new ConsentService(consumers, new InMemoryConsentRepository(), e -> {}, clock);
  private final InMemorySubscriptions repo = new InMemorySubscriptions();
  private final SubscriptionManager manager =
      new SubscriptionManager(repo, consumers, consents, clock);
  private final BrandId brandA = BrandId.random();
  private Consumer ana;

  @BeforeEach
  void setUp() {
    TenantContext.set(brandA);
    ana = consumers.register(new Email("ana@example.test"), null, new LanguageTag("es"));
    manager.createService("newsletter", "Boletín", null);
    manager.createService("news", "Novedades", null);
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  private void optInSubscriptions() {
    consents.change(ana.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");
    consents.change(ana.id(), ConsentChannel.SUBSCRIPTIONS, true, ConsentSource.WEB, "consumer");
  }

  @Test
  void subscribingRequiresSubscriptionsOptIn() {
    assertThatThrownBy(() -> manager.subscribe(ana.id(), "newsletter"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("SUBSCRIPTIONS");

    optInSubscriptions();
    Subscription s = manager.subscribe(ana.id(), "newsletter");

    assertThat(s.isActive()).isTrue();
    assertThat(manager.activeServiceCodes(ana.id())).containsExactly("newsletter");
    assertThat(manager.isSubscribedTo(ana.id(), "newsletter")).isTrue();
    assertThat(manager.isSubscribedTo(ana.id(), "news")).isFalse();
  }

  @Test
  void unsubscribeKeepsHistoryAndAllowsResubscribe() {
    optInSubscriptions();
    manager.subscribe(ana.id(), "newsletter");
    assertThatThrownBy(() -> manager.subscribe(ana.id(), "newsletter"))
        .isInstanceOf(ConflictException.class);

    manager.unsubscribe(ana.id(), "newsletter");
    manager.unsubscribe(ana.id(), "newsletter");
    assertThat(manager.activeServiceCodes(ana.id())).isEmpty();

    manager.subscribe(ana.id(), "newsletter");
    assertThat(manager.subscriptions(ana.id())).hasSize(2);
  }

  @Test
  void catalogIsPerBrand() {
    assertThatThrownBy(() -> manager.createService("news", "Otra", null))
        .isInstanceOf(ConflictException.class);
    TenantContext.runAs(
        BrandId.random(),
        () -> {
          assertThat(manager.services()).isEmpty();
          manager.createService("news", "Novedades B", null);
        });
    assertThat(manager.services())
        .extracting(SubscriptionService::code)
        .contains("news", "newsletter");
  }

  @Test
  void updateServiceAndErrors() {
    SubscriptionService news = repo.findServiceByCode("news").orElseThrow();
    manager.updateService(news.id(), "Novedades", "desc", false);
    optInSubscriptions();

    assertThatThrownBy(() -> manager.subscribe(ana.id(), "news"))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> manager.subscribe(ana.id(), "missing"))
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> manager.updateService(SubscriptionServiceId.random(), "x", null, true))
        .isInstanceOf(NotFoundException.class);
    TenantContext.clear();
    assertThat(manager.activeServiceCodes(ana.id())).isEmpty();
  }
}
