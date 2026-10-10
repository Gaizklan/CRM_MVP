package org.example.crm.rules.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.application.ConsumerRepository;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.PreferenceChange;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.rules.domain.RuleValidationException;
import org.example.crm.rules.infrastructure.JacksonRuleReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);
  private final BrandId brand = BrandId.random();
  private Consumer consumer;
  private RuleEngine engine;

  @BeforeEach
  void setUp() {
    TenantContext.set(brand);
    consumer =
        Consumer.register(
            brand,
            new Email("ana@example.test"),
            new PhoneNumber("+34600000000"),
            new LanguageTag("es"),
            clock);
    consumer.setPreference("budget", PreferenceValue.of(new BigDecimal("10")), "test", clock);
    ConsumerRepository repo =
        new ConsumerRepository() {
          @Override
          public Optional<Consumer> findById(ConsumerId id) {
            return id.equals(consumer.id()) ? Optional.of(consumer) : Optional.empty();
          }

          @Override
          public Optional<Consumer> findByEmail(Email email) {
            return Optional.empty();
          }

          @Override
          public PageResult<Consumer> search(ConsumerSearchCriteria c, PageQuery p) {
            return new PageResult<>(java.util.List.of(), 0, 1, 0);
          }

          @Override
          public Consumer save(Consumer c, java.util.List<PreferenceChange> h) {
            return c;
          }

          @Override
          public java.util.List<PreferenceChange> preferenceHistory(ConsumerId id) {
            return java.util.List.of();
          }
        };
    engine =
        new RuleEngine(
            new JacksonRuleReader(),
            new ConsumerService(repo, e -> {}, clock),
            (id, channel) -> channel == ConsentChannel.CRM_PROGRAM || channel == ConsentChannel.SMS,
            id -> Set.of("news"),
            (id, type, since) -> type.equals("LOGIN"),
            clock);
  }

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void buildsContextFromConsumerConsentsSubscriptionsAndEvents() {
    var ctx = engine.contextFor(consumer.id());

    assertThat(ctx.profile())
        .containsEntry("email", "ana@example.test")
        .containsEntry("phone", "+34600000000")
        .containsEntry("status", "ACTIVE")
        .doesNotContainKey("segment");
    assertThat(ctx.allowedChannels())
        .containsExactlyInAnyOrder(ConsentChannel.CRM_PROGRAM, ConsentChannel.SMS);
    assertThat(ctx.subscriptions()).containsExactly("news");
    assertThat(ctx.now()).isEqualTo(clock.instant());
  }

  @Test
  void evaluatesJsonRules() {
    String rule =
        """
        {"and":[
          {"eq":{"field":"consent.SMS","value":true}},
          {"subscribedTo":{"service":"news"}},
          {"lt":{"field":"preference.budget","value":20}},
          {"eventOccurred":{"type":"LOGIN","within":"P1D"}}
        ]}
        """;

    assertThat(engine.evaluate(engine.parse(rule), consumer.id())).isTrue();
    assertThat(engine.validate(rule)).isEmpty();
    assertThat(engine.validate("{\"eq\":1}")).isNotEmpty();
    assertThat(engine.validate("no-json")).singleElement().asString().contains("JSON no válido");
    assertThatThrownBy(() -> engine.parse("{}")).isInstanceOf(RuleValidationException.class);
  }
}
