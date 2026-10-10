package org.example.crm.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.DomainEventHandler;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.infrastructure.events.OutboxRelay;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.rules.application.RuleEngine;
import org.example.crm.tracking.application.UserEventRecorder;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DomainEventsIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Handler que falla las dos primeras veces para probar los reintentos del relay. */
  @TestConfiguration
  static class FlakyHandlerConfig {
    static final AtomicInteger calls = new AtomicInteger();
    static volatile UUID failFor;

    @Bean
    DomainEventHandler<ConsumerProfileChanged> flakyHandler() {
      return new DomainEventHandler<>() {
        @Override
        public Class<ConsumerProfileChanged> eventType() {
          return ConsumerProfileChanged.class;
        }

        @Override
        public void handle(ConsumerProfileChanged event, UUID eventId) {
          if (event.consumerId().value().equals(failFor) && calls.incrementAndGet() <= 2) {
            throw new IllegalStateException("fallo simulado");
          }
        }
      };
    }
  }

  /** Handler que usa JPA (consulta del consumidor) para verificar la Marca de la sesión. */
  @TestConfiguration
  static class JpaHandlerConfig {
    static final java.util.List<String> seenEmails =
        java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    @Bean
    DomainEventHandler<org.example.crm.consumer.domain.ConsentChanged> jpaHandler(
        ConsumerService consumers) {
      return new DomainEventHandler<>() {
        @Override
        public Class<org.example.crm.consumer.domain.ConsentChanged> eventType() {
          return org.example.crm.consumer.domain.ConsentChanged.class;
        }

        @Override
        public void handle(org.example.crm.consumer.domain.ConsentChanged event, UUID eventId) {
          seenEmails.add(consumers.get(event.consumerId()).email().value());
        }
      };
    }
  }

  @Autowired ConsumerService consumers;
  @Autowired ConsentService consents;
  @Autowired UserEventRecorder events;
  @Autowired RuleEngine rules;
  @Autowired OutboxRelay relay;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate tx;
  @Autowired BrandLookup brands;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired MockMvc mvc;

  BrandId brandA;

  @BeforeEach
  void setUp() {
    brandA = brands.findByCode("marca-a").orElseThrow().id();
    relay.relayPending();
  }

  private Consumer newConsumer() {
    return TenantContext.callAs(
        brandA,
        () ->
            consumers.register(
                new Email("ev-" + System.nanoTime() + "@example.test"),
                null,
                new LanguageTag("es")));
  }

  private int pendingFor(UUID consumer) {
    return jdbc.queryForObject(
        "select count(*) from domain_event_outbox where published_at is null and"
            + " payload->'consumerId'->>'value' = ?",
        Integer.class,
        consumer.toString());
  }

  @Test
  void domainEventsAreStoredInOutboxWithBusinessDataAndRelayed() {
    Consumer c = newConsumer();
    TenantContext.runAs(
        brandA,
        () -> {
          consumers.setPreference(
              c.id(), "budget", PreferenceValue.of(new BigDecimal("5")), "test");
          consents.change(c.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");
        });
    assertThat(pendingFor(c.id().value())).isEqualTo(2);

    assertThat(relay.relayPending()).isGreaterThanOrEqualTo(2);

    assertThat(pendingFor(c.id().value())).isZero();
    var page =
        TenantContext.callAs(
            brandA,
            () ->
                events.search(
                    new UserEventCriteria(c.id(), UserEventType.PREFERENCES_CHANGED, null, null),
                    new PageQuery(0, 10)));
    assertThat(page.totalElements()).isEqualTo(2);
    assertThat(relay.relayPending()).as("cada evento se entrega una vez").isZero();
    assertThat(JpaHandlerConfig.seenEmails)
        .as("los handlers con JPA ven los datos de la Marca del evento")
        .contains(c.email().value());
  }

  @Test
  void rolledBackTransactionLeavesNoEvent() {
    Consumer c = newConsumer();

    // La Marca se fija antes de abrir la transacción (Hibernate la asocia a la sesión).
    assertThatThrownBy(
            () ->
                TenantContext.runAs(
                    brandA,
                    () ->
                        tx.executeWithoutResult(
                            status -> {
                              consumers.setPreference(
                                  c.id(), "vip", PreferenceValue.of(true), "test");
                              throw new IllegalStateException("rollback");
                            })))
        .hasMessage("rollback");

    assertThat(pendingFor(c.id().value())).isZero();
    assertThat(TenantContext.callAs(brandA, () -> consumers.get(c.id()).preference("vip")))
        .isEmpty();
  }

  @Test
  void failedHandlerIsRetriedUntilItSucceeds() {
    Consumer c = newConsumer();
    FlakyHandlerConfig.failFor = c.id().value();
    FlakyHandlerConfig.calls.set(0);
    TenantContext.runAs(
        brandA, () -> consumers.setPreference(c.id(), "x", PreferenceValue.of("1"), "test"));

    relay.relayPending();
    assertThat(pendingFor(c.id().value())).isEqualTo(1);
    relay.relayPending();
    relay.relayPending();

    assertThat(pendingFor(c.id().value())).isZero();
    assertThat(
            jdbc.queryForObject(
                "select attempts from domain_event_outbox where payload->'consumerId'->>'value' ="
                    + " ?",
                Integer.class,
                c.id().value().toString()))
        .isEqualTo(3);
    // El handler de tracking es idempotente: un único evento de usuario pese a los reintentos.
    assertThat(
            TenantContext.callAs(
                    brandA,
                    () ->
                        events.search(
                            new UserEventCriteria(
                                c.id(), UserEventType.PREFERENCES_CHANGED, null, null),
                            new PageQuery(0, 10)))
                .totalElements())
        .isEqualTo(1);
  }

  @Test
  void userEventsAreQueryableAndAvailableToRules() throws Exception {
    Consumer c = newConsumer();
    Instant now = Instant.now();
    TenantContext.runAs(
        brandA,
        () -> {
          events.record(c.id(), UserEventType.LOGIN, now.minus(Duration.ofDays(5)), null);
          events.record(
              c.id(),
              UserEventType.EMAIL_OPENED,
              now.minus(Duration.ofHours(20)),
              Map.of("campaignId", "c1"));
          events.record(
              c.id(),
              UserEventType.EMAIL_CLICKED,
              now.minus(Duration.ofHours(1)),
              Map.of("zone", "z1"));
        });
    TenantContext.runAs(
        brands.findByCode("marca-b").orElseThrow().id(),
        () -> events.record(c.id(), UserEventType.LOGOUT, now, null));

    TenantContext.runAs(
        brandA,
        () -> {
          var lastDay =
              events.search(
                  new UserEventCriteria(
                      c.id(), null, now.minus(Duration.ofDays(1)), now.plusSeconds(1)),
                  new PageQuery(0, 10));
          assertThat(lastDay.content())
              .extracting(e -> e.type())
              .containsExactly(UserEventType.EMAIL_CLICKED, UserEventType.EMAIL_OPENED);
          assertThat(
                  events
                      .search(
                          new UserEventCriteria(c.id(), UserEventType.LOGOUT, null, null),
                          new PageQuery(0, 5))
                      .totalElements())
              .as("eventos de otra Marca no visibles")
              .isZero();
          assertThat(
                  rules.evaluate(
                      rules.parse(
                          "{\"eventOccurred\":{\"type\":\"EMAIL_OPENED\",\"within\":\"P1D\"}}"),
                      c.id()))
              .isTrue();
          assertThat(
                  rules.evaluate(
                      rules.parse("{\"eventOccurred\":{\"type\":\"LOGIN\",\"within\":\"P2D\"}}"),
                      c.id()))
              .isFalse();
        });

    if (userRepository.findByUsername("gestor.events").isEmpty()) {
      users.create(
          new InternalUserCommand(
              "gestor.events", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
    }
    mvc.perform(
            get("/api/user-events")
                .header("X-Brand", "marca-a")
                .param("consumerId", c.id().toString())
                .param("type", "EMAIL_CLICKED")
                .with(httpBasic("gestor.events", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].attributes.zone").value("z1"));
  }
}
