package org.example.crm.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.application.tenant.TenantNotSetException;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.domain.ConsentChanged;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.tracking.domain.UserEvent;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserEventRecorderTest {

  static final class InMemoryEvents implements UserEventRepository {
    final List<UserEvent> events = new ArrayList<>();

    @Override
    public void save(UserEvent event) {
      if (event.sourceEventId() != null
          && events.stream().anyMatch(e -> event.sourceEventId().equals(e.sourceEventId()))) {
        return;
      }
      events.add(event);
    }

    private List<UserEvent> visible(UserEventCriteria c) {
      BrandId brand = TenantContext.require();
      return events.stream()
          .filter(e -> e.brandId().equals(brand))
          .filter(e -> c.consumerId() == null || e.consumerId().equals(c.consumerId()))
          .filter(e -> c.type() == null || e.type() == c.type())
          .filter(e -> c.from() == null || !e.occurredAt().isBefore(c.from()))
          .filter(e -> c.to() == null || e.occurredAt().isBefore(c.to()))
          .sorted(Comparator.comparing(UserEvent::occurredAt).reversed())
          .toList();
    }

    @Override
    public PageResult<UserEvent> search(UserEventCriteria criteria, PageQuery page) {
      List<UserEvent> all = visible(criteria);
      return new PageResult<>(
          all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(),
          page.page(),
          page.size(),
          all.size());
    }

    @Override
    public boolean existsSince(ConsumerId consumerId, UserEventType type, Instant since) {
      return !visible(new UserEventCriteria(consumerId, type, since, null)).isEmpty();
    }
  }

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T10:00:00Z"), ZoneOffset.UTC);
  private final InMemoryEvents repo = new InMemoryEvents();
  private final UserEventRecorder recorder = new UserEventRecorder(repo, clock);
  private final BrandId brand = BrandId.random();
  private final ConsumerId ana = ConsumerId.random();

  @BeforeEach
  void setBrand() {
    TenantContext.set(brand);
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void recordsAndQueriesByConsumerTypeAndRange() {
    recorder.record(ana, UserEventType.LOGIN, clock.instant().minus(Duration.ofDays(3)), null);
    recorder.record(ana, UserEventType.EMAIL_OPENED, null, Map.of("campaignId", "c1"));
    recorder.record(ConsumerId.random(), UserEventType.LOGIN, null, null);

    assertThat(
            recorder
                .search(new UserEventCriteria(ana, null, null, null), new PageQuery(0, 10))
                .content())
        .extracting(UserEvent::type)
        .containsExactly(UserEventType.EMAIL_OPENED, UserEventType.LOGIN);
    assertThat(
            recorder
                .search(
                    new UserEventCriteria(
                        null, UserEventType.LOGIN, clock.instant().minus(Duration.ofDays(1)), null),
                    new PageQuery(0, 10))
                .totalElements())
        .isEqualTo(1);
  }

  @Test
  void feedsEventOccurredForRules() {
    recorder.record(
        ana, UserEventType.EMAIL_OPENED, clock.instant().minus(Duration.ofHours(30)), null);

    assertThat(
            recorder.occurredSince(ana, "EMAIL_OPENED", clock.instant().minus(Duration.ofDays(2))))
        .isTrue();
    assertThat(
            recorder.occurredSince(ana, "EMAIL_OPENED", clock.instant().minus(Duration.ofDays(1))))
        .isFalse();
    assertThat(recorder.occurredSince(ana, "UNKNOWN", Instant.EPOCH)).isFalse();
    assertThat(
            TenantContext.callAs(
                BrandId.random(), () -> recorder.occurredSince(ana, "EMAIL_OPENED", Instant.EPOCH)))
        .isFalse();
    TenantContext.clear();
    assertThat(recorder.occurredSince(ana, "EMAIL_OPENED", Instant.EPOCH)).isFalse();
    assertThatThrownBy(() -> recorder.record(ana, UserEventType.LOGIN, null, null))
        .isInstanceOf(TenantNotSetException.class);
  }

  @Test
  void derivesPreferencesChangedFromDomainEventsIdempotently() {
    var profile = PreferenceEventsHandler.profileChanges(recorder);
    var consent = PreferenceEventsHandler.consentChanges(recorder);
    UUID e1 = UUID.randomUUID();
    ConsumerProfileChanged prefs =
        new ConsumerProfileChanged(
            brand, ana, Set.of("preference.b", "preference.a", "segment"), clock.instant());

    profile.handle(prefs, e1);
    profile.handle(prefs, e1);
    profile.handle(
        new ConsumerProfileChanged(brand, ana, Set.of("segment"), clock.instant()),
        UUID.randomUUID());
    consent.handle(
        new ConsentChanged(
            brand, ana, ConsentChannel.SMS, false, ConsentSource.EMAIL, clock.instant()),
        UUID.randomUUID());

    assertThat(profile.eventType()).isEqualTo(ConsumerProfileChanged.class);
    assertThat(consent.eventType()).isEqualTo(ConsentChanged.class);
    assertThat(repo.events).hasSize(2).allMatch(e -> e.type() == UserEventType.PREFERENCES_CHANGED);
    assertThat(repo.events.get(0).attributes()).containsEntry("preferences", "a,b");
    assertThat(repo.events.get(1).attributes())
        .containsEntry("consentChannel", "SMS")
        .containsEntry("granted", "false")
        .containsEntry("source", "EMAIL");
  }
}
