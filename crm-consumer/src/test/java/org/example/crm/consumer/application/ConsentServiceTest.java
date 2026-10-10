package org.example.crm.consumer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.consumer.domain.ConsentChanged;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentServiceTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final List<DomainEvent> events = new ArrayList<>();
  private final InMemoryConsumerRepository consumerRepo = new InMemoryConsumerRepository();
  private final ConsumerService consumers = new ConsumerService(consumerRepo, events::add, clock);
  private final InMemoryConsentRepository consentRepo = new InMemoryConsentRepository();
  private final ConsentService service =
      new ConsentService(consumers, consentRepo, events::add, clock);
  private final BrandId brandA = BrandId.random();
  private Consumer ana;

  @BeforeEach
  void setUp() {
    TenantContext.set(brandA);
    ana = consumers.register(new Email("ana@example.test"), null, new LanguageTag("es"));
    events.clear();
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void checkerRequiresChannelAndProgram() {
    assertThat(service.isAllowed(ana.id(), ConsentChannel.EMAIL)).isFalse();

    service.change(ana.id(), ConsentChannel.EMAIL, true, ConsentSource.WEB, "consumer");
    assertThat(service.isAllowed(ana.id(), ConsentChannel.EMAIL)).isFalse();

    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");
    assertThat(service.isAllowed(ana.id(), ConsentChannel.EMAIL)).isTrue();
    assertThat(service.isAllowed(ana.id(), ConsentChannel.SMS)).isFalse();
  }

  @Test
  void changesArePersistedAuditedAndPublished() {
    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.IMPORT, "import:1");
    service.change(ana.id(), ConsentChannel.SMS, true, ConsentSource.ADMIN, "admin:x");
    service.change(ana.id(), ConsentChannel.SMS, true, ConsentSource.ADMIN, "admin:x");
    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, false, ConsentSource.EMAIL, "consumer");

    assertThat(service.history(ana.id()))
        .hasSize(4)
        .first()
        .satisfies(c -> assertThat(c.channel()).isEqualTo(ConsentChannel.SMS));
    assertThat(events).hasSize(4).allMatch(e -> e instanceof ConsentChanged);
    assertThat(service.current(ana.id()).isAllowed(ConsentChannel.SMS)).isFalse();
  }

  @Test
  void unknownOrOtherBrandConsumerIsNeverAllowed() {
    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");

    assertThat(service.isAllowed(ConsumerId.random(), ConsentChannel.CRM_PROGRAM)).isFalse();
    assertThat(
            TenantContext.callAs(
                BrandId.random(), () -> service.isAllowed(ana.id(), ConsentChannel.CRM_PROGRAM)))
        .isFalse();
    TenantContext.clear();
    assertThat(service.isAllowed(ana.id(), ConsentChannel.CRM_PROGRAM)).isFalse();
    TenantContext.set(brandA);
    assertThatThrownBy(
            () ->
                service.change(
                    ConsumerId.random(), ConsentChannel.SMS, true, ConsentSource.WEB, "x"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void withdrawnConsumerIsNeverAllowedAndCannotOptIn() {
    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");
    Consumer stored = consumers.get(ana.id());
    stored.withdraw(clock);
    consumerRepo.save(stored, List.of());

    assertThat(service.isAllowed(ana.id(), ConsentChannel.CRM_PROGRAM)).isFalse();
    assertThatThrownBy(
            () -> service.change(ana.id(), ConsentChannel.EMAIL, true, ConsentSource.WEB, "x"))
        .isInstanceOf(DomainException.class);
    service.change(ana.id(), ConsentChannel.CRM_PROGRAM, false, ConsentSource.WEB, "consumer");
  }
}
