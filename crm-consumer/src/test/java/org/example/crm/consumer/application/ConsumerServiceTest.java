package org.example.crm.consumer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.application.tenant.TenantNotSetException;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainEvent;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.consumer.domain.ProfileUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsumerServiceTest {

  private final List<DomainEvent> published = new ArrayList<>();
  private final DomainEventPublisher publisher = published::add;
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final ConsumerService service =
      new ConsumerService(new InMemoryConsumerRepository(), publisher, clock);
  private final BrandId brandA = BrandId.random();
  private final BrandId brandB = BrandId.random();

  @BeforeEach
  void setBrand() {
    TenantContext.set(brandA);
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  private Consumer register(String email) {
    return service.register(new Email(email), null, new LanguageTag("es"));
  }

  @Test
  void emailIsUniquePerBrand() {
    register("ana@example.test");

    assertThatThrownBy(() -> register("ANA@example.test")).isInstanceOf(ConflictException.class);
    Consumer inB = TenantContext.callAs(brandB, () -> register("ana@example.test"));
    assertThat(inB.brandId()).isEqualTo(brandB);
  }

  @Test
  void updatingEmailToExistingOneConflicts() {
    register("ana@example.test");
    Consumer luis = register("luis@example.test");

    assertThatThrownBy(
            () ->
                service.updateProfile(
                    luis.id(),
                    new ProfileUpdate(new Email("ana@example.test"), null, null, null, null)))
        .isInstanceOf(ConflictException.class);
    service.updateProfile(
        luis.id(), new ProfileUpdate(new Email("luis@example.test"), null, null, null, null));
  }

  @Test
  void profileAndPreferenceChangesPublishEvents() {
    Consumer ana = register("ana@example.test");

    service.updateProfile(ana.id(), new ProfileUpdate(null, null, null, Optional.of("S1"), null));
    service.setPreference(ana.id(), "channel", PreferenceValue.of("email"), "admin:x");
    service.removePreference(ana.id(), "channel", "admin:x");

    assertThat(published)
        .hasSize(3)
        .allSatisfy(e -> assertThat(e).isInstanceOf(ConsumerProfileChanged.class));
    assertThat(((ConsumerProfileChanged) published.get(0)).changedFields())
        .containsExactly("segment");
    assertThat(((ConsumerProfileChanged) published.get(1)).changedFields())
        .containsExactly("preference.channel");
    assertThat(service.preferenceHistory(ana.id())).hasSize(2);
  }

  @Test
  void otherBrandCannotSeeConsumer() {
    Consumer ana = register("ana@example.test");

    TenantContext.runAs(
        brandB,
        () -> {
          assertThatThrownBy(() -> service.get(ana.id())).isInstanceOf(NotFoundException.class);
          assertThat(service.search(ConsumerSearchCriteria.all(), new PageQuery(0, 10)).content())
              .isEmpty();
        });
    assertThat(service.search(ConsumerSearchCriteria.all(), new PageQuery(0, 10)).totalElements())
        .isEqualTo(1);
  }

  @Test
  void requiresBrand() {
    TenantContext.clear();

    assertThatThrownBy(() -> register("ana@example.test"))
        .isInstanceOf(TenantNotSetException.class);
    assertThatThrownBy(() -> service.search(ConsumerSearchCriteria.all(), new PageQuery(0, 1)))
        .isInstanceOf(TenantNotSetException.class);
  }
}
