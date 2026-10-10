package org.example.crm.consumer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;

class ConsumerTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final BrandId brand = BrandId.random();

  private Consumer newConsumer() {
    Consumer c =
        Consumer.register(brand, new Email("ana@example.test"), null, new LanguageTag("es"), clock);
    c.pullEvents();
    return c;
  }

  private ConsumerProfileChanged singleEvent(Consumer c) {
    var events = c.pullEvents();
    assertThat(events).hasSize(1);
    return (ConsumerProfileChanged) events.get(0);
  }

  @Test
  void registerCreatesActiveConsumer() {
    Consumer c =
        Consumer.register(
            brand,
            new Email("ana@example.test"),
            new PhoneNumber("+34600000000"),
            new LanguageTag("es"),
            clock);

    assertThat(c.status()).isEqualTo(ConsumerStatus.ACTIVE);
    assertThat(c.brandId()).isEqualTo(brand);
    assertThat(c.registeredAt()).isEqualTo(clock.instant());
    assertThat(c.phone()).contains(new PhoneNumber("+34600000000"));
    assertThat(c.segment()).isEmpty();
    assertThat(c.pullEvents()).isEmpty();
  }

  @Test
  void profileUpdatePublishesChangedFields() {
    Consumer c = newConsumer();
    Map<String, String> attrs = new HashMap<>();
    attrs.put("city", "Ciudad 1");

    c.updateProfile(
        new ProfileUpdate(
            new Email("ana2@example.test"),
            Optional.of(new PhoneNumber("+34611111111")),
            new LanguageTag("en"),
            Optional.of("S1"),
            attrs),
        clock);

    ConsumerProfileChanged event = singleEvent(c);
    assertThat(event.changedFields())
        .containsExactlyInAnyOrder("email", "phone", "language", "segment", "attribute.city");
    assertThat(event.brandId()).isEqualTo(brand);
    assertThat(event.consumerId()).isEqualTo(c.id());
    assertThat(event.occurredAt()).isEqualTo(clock.instant());
    assertThat(c.segment()).contains("S1");
    assertThat(c.attributes()).containsEntry("city", "Ciudad 1");
  }

  @Test
  void unchangedProfileDoesNotPublish() {
    Consumer c = newConsumer();

    c.updateProfile(
        new ProfileUpdate(new Email("ana@example.test"), Optional.empty(), null, null, Map.of()),
        clock);
    c.updateProfile(ProfileUpdate.none(), clock);

    assertThat(c.pullEvents()).isEmpty();
  }

  @Test
  void removesAttributesAndClearsOptionalFields() {
    Consumer c = newConsumer();
    c.updateProfile(
        new ProfileUpdate(
            null,
            Optional.of(new PhoneNumber("+34600000000")),
            null,
            Optional.of("S1"),
            Map.of("a", "1")),
        clock);
    c.pullEvents();
    Map<String, String> removal = new HashMap<>();
    removal.put("a", null);

    c.updateProfile(
        new ProfileUpdate(null, Optional.empty(), null, Optional.empty(), removal), clock);

    assertThat(singleEvent(c).changedFields())
        .containsExactlyInAnyOrder("phone", "segment", "attribute.a");
    assertThat(c.attributes()).isEmpty();
    assertThat(c.phone()).isEmpty();
    assertThat(c.segment()).isEmpty();
  }

  @Test
  void preferencesAreTypedWithHistory() {
    Consumer c = newConsumer();

    c.setPreference("newsletterFrequency", PreferenceValue.of("weekly"), "consumer", clock);
    c.setPreference("newsletterFrequency", PreferenceValue.of("monthly"), "admin:ana", clock);
    c.setPreference("newsletterFrequency", PreferenceValue.of("monthly"), "admin:ana", clock);
    c.setPreference("budget", PreferenceValue.of(new BigDecimal("10")), "consumer", clock);
    c.removePreference("budget", "consumer", clock);
    c.removePreference("missing", "consumer", clock);

    var history = c.pullPreferenceChanges();
    assertThat(history).hasSize(4);
    assertThat(history.get(0).previous()).isEmpty();
    assertThat(history.get(1).previous()).contains(PreferenceValue.of("weekly"));
    assertThat(history.get(1).current()).contains(PreferenceValue.of("monthly"));
    assertThat(history.get(1).changedBy()).isEqualTo("admin:ana");
    assertThat(history.get(3).current()).isEmpty();
    assertThat(c.pullPreferenceChanges()).isEmpty();
    assertThat(singleEvent(c).changedFields())
        .containsExactlyInAnyOrder("preference.newsletterFrequency", "preference.budget");
    assertThat(c.preference("newsletterFrequency")).contains(PreferenceValue.of("monthly"));
    assertThat(c.preferences()).containsOnlyKeys("newsletterFrequency");
  }

  @Test
  void validatesKeysSegmentsAndActor() {
    Consumer c = newConsumer();

    assertThatThrownBy(() -> c.setPreference("1bad", PreferenceValue.of("x"), "a", clock))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> c.setPreference("ok", PreferenceValue.of("x"), " ", clock))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(
            () ->
                c.updateProfile(
                    new ProfileUpdate(null, null, null, Optional.of("S 1"), null), clock))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(
            () ->
                c.updateProfile(
                    new ProfileUpdate(null, null, null, null, Map.of("k", "x".repeat(1001))),
                    clock))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void statusTransitions() {
    Consumer c = newConsumer();

    c.lock(clock);
    assertThat(c.status()).isEqualTo(ConsumerStatus.LOCKED);
    c.lock(clock);
    c.unlock(clock);
    assertThat(c.status()).isEqualTo(ConsumerStatus.ACTIVE);
    c.unlock(clock);
    assertThat(singleEvent(c).changedFields()).containsExactly("status");

    c.withdraw(clock);
    assertThat(c.status()).isEqualTo(ConsumerStatus.WITHDRAWN);
    assertThatThrownBy(() -> c.setPreference("x", PreferenceValue.of(true), "a", clock))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> c.lock(clock)).isInstanceOf(DomainException.class);
  }

  @Test
  void restoreDoesNotEmitEventsAndEqualityById() {
    Consumer original = newConsumer();
    Consumer restored =
        Consumer.restore(
            original.id(),
            brand,
            new Email("otro@example.test"),
            null,
            new LanguageTag("es"),
            "S1",
            ConsumerStatus.LOCKED,
            clock.instant(),
            Map.of("a", "1"),
            Map.of("p", PreferenceValue.of(false)));

    assertThat(restored).isEqualTo(original).hasSameHashCodeAs(original);
    assertThat(restored.pullEvents()).isEmpty();
    assertThat(restored.preference("p")).contains(PreferenceValue.of(false));
  }

  @Test
  void eventIsADomainEvent() {
    DomainEvent e =
        new ConsumerProfileChanged(
            brand, ConsumerId.random(), java.util.Set.of("x"), clock.instant());
    assertThat(e.brandId()).isEqualTo(brand);
    assertThat(ConsumerSearchCriteria.all().email()).isNull();
  }
}
