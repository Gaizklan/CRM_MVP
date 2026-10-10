package org.example.crm.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.example.crm.common.application.tenant.BrandService;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.application.SubscriptionManager;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.consumer.domain.ProfileUpdate;
import org.example.crm.rules.application.RuleEngine;
import org.example.crm.rules.application.RuleNotTranslatableException;
import org.example.crm.rules.application.Segmenter;
import org.example.crm.rules.domain.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Equivalencia: para cada regla, los consumidores seleccionados por SQL (PostgreSQL real) son
 * exactamente los que cumplen la regla evaluada en memoria, sobre un conjunto semilla variado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleSqlEquivalenceIT {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired BrandService brands;
  @Autowired ConsumerService consumers;
  @Autowired org.example.crm.consumer.application.ConsumerRepository consumerRepository;
  @Autowired ConsentService consents;
  @Autowired SubscriptionManager subscriptions;
  @Autowired RuleEngine engine;
  @Autowired Segmenter segmenter;

  BrandId brand;
  List<ConsumerId> seed;

  /**
   * Consumidor semilla: segmento, idioma, teléfono, atributos, preferencias, canales, servicios.
   */
  record Spec(
      String segment,
      String language,
      String phone,
      Map<String, String> attributes,
      Map<String, PreferenceValue> preferences,
      Set<ConsentChannel> optIns,
      Set<String> services,
      boolean withdraw) {}

  @BeforeAll
  void seedData() {
    brand = brands.create("eq-" + System.nanoTime() % 1_000_000, "Marca Eq", null, "es").id();
    BrandId other =
        brands.create("eqb-" + System.nanoTime() % 1_000_000, "Marca Eq B", null, "es").id();
    TenantContext.runAs(
        brand,
        () -> {
          subscriptions.createService("newsletter", "Boletín", null);
          subscriptions.createService("news", "Novedades", null);
        });
    List<Spec> specs =
        List.of(
            new Spec(
                "S1",
                "es",
                "+34600000001",
                Map.of("city", "Ciudad Norte"),
                prefs(
                    "budget",
                    num("150.5"),
                    "vip",
                    PreferenceValue.of(true),
                    "birthday",
                    PreferenceValue.of(LocalDate.of(1990, 5, 1)),
                    "channel",
                    PreferenceValue.of("email")),
                Set.of(
                    ConsentChannel.CRM_PROGRAM, ConsentChannel.EMAIL, ConsentChannel.SUBSCRIPTIONS),
                Set.of("newsletter"),
                false),
            new Spec(
                "S1",
                "en",
                null,
                Map.of("city", "Ciudad Sur"),
                prefs("budget", num("99"), "vip", PreferenceValue.of(false)),
                Set.of(
                    ConsentChannel.CRM_PROGRAM, ConsentChannel.SMS, ConsentChannel.SUBSCRIPTIONS),
                Set.of("news", "newsletter"),
                false),
            new Spec(
                "S2",
                "es",
                "+34600000003",
                Map.of(),
                prefs(
                    "budget",
                    num("100"),
                    "channel",
                    PreferenceValue.of("sms"),
                    "birthday",
                    PreferenceValue.of(LocalDate.of(2001, 12, 31))),
                Set.of(ConsentChannel.EMAIL),
                Set.of(),
                false),
            new Spec(
                null,
                "fr",
                null,
                Map.of("city", "Norte"),
                prefs("budget", PreferenceValue.of("alto"), "vip", PreferenceValue.of("true")),
                Set.of(),
                Set.of(),
                false),
            new Spec(
                "S1",
                "es",
                null,
                Map.of(),
                prefs("budget", num("150.50")),
                Set.of(ConsentChannel.CRM_PROGRAM, ConsentChannel.EMAIL, ConsentChannel.PUSH),
                Set.of(),
                true),
            new Spec(
                "S3",
                "es-ES",
                "+34600000006",
                Map.of("city", "ciudad norte", "zip", "00001"),
                prefs("birthday", PreferenceValue.of("1990-05-01")),
                Set.of(ConsentChannel.CRM_PROGRAM, ConsentChannel.MMS),
                Set.of(),
                false));
    seed = specs.stream().map(this::create).toList();
    // Ruido en otra Marca que nunca debe aparecer.
    TenantContext.runAs(
        other,
        () -> {
          Consumer c =
              consumers.register(new Email("otra@example.test"), null, new LanguageTag("es"));
          consumers.updateProfile(
              c.id(), new ProfileUpdate(null, null, null, Optional.of("S1"), null));
        });
  }

  private static Map<String, PreferenceValue> prefs(Object... kv) {
    Map<String, PreferenceValue> map = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      map.put((String) kv[i], (PreferenceValue) kv[i + 1]);
    }
    return map;
  }

  private static PreferenceValue num(String v) {
    return PreferenceValue.of(new BigDecimal(v));
  }

  private ConsumerId create(Spec s) {
    return TenantContext.callAs(
        brand,
        () -> {
          Consumer c =
              consumers.register(
                  new Email("eq-" + System.nanoTime() + "@example.test"),
                  s.phone() == null ? null : new PhoneNumber(s.phone()),
                  new LanguageTag(s.language()));
          consumers.updateProfile(
              c.id(),
              new ProfileUpdate(
                  null, null, null, Optional.ofNullable(s.segment()), s.attributes()));
          s.preferences().forEach((k, v) -> consumers.setPreference(c.id(), k, v, "seed"));
          s.optIns().forEach(ch -> consents.change(c.id(), ch, true, ConsentSource.IMPORT, "seed"));
          s.services().forEach(code -> subscriptions.subscribe(c.id(), code));
          if (s.withdraw()) {
            Consumer stored = consumers.get(c.id());
            stored.withdraw(java.time.Clock.systemUTC());
            consumerRepository.save(stored, List.of());
          }
          return c.id();
        });
  }

  static Stream<String> rules() {
    return Stream.of(
        "{\"eq\":{\"field\":\"segment\",\"value\":\"S1\"}}",
        "{\"ne\":{\"field\":\"segment\",\"value\":\"S1\"}}",
        "{\"in\":{\"field\":\"language\",\"values\":[\"es\",\"fr\"]}}",
        "{\"exists\":{\"field\":\"segment\"}}",
        "{\"not\":{\"exists\":{\"field\":\"phone\"}}}",
        "{\"eq\":{\"field\":\"status\",\"value\":\"ACTIVE\"}}",
        "{\"contains\":{\"field\":\"segment\",\"value\":\"S\"}}",
        "{\"eq\":{\"field\":\"attribute.city\",\"value\":\"Ciudad Norte\"}}",
        "{\"ne\":{\"field\":\"attribute.city\",\"value\":\"Ciudad Norte\"}}",
        "{\"contains\":{\"field\":\"attribute.city\",\"value\":\"Norte\"}}",
        "{\"exists\":{\"field\":\"attribute.zip\"}}",
        "{\"gt\":{\"field\":\"preference.budget\",\"value\":100}}",
        "{\"lt\":{\"field\":\"preference.budget\",\"value\":100}}",
        "{\"eq\":{\"field\":\"preference.budget\",\"value\":150.5}}",
        "{\"ne\":{\"field\":\"preference.budget\",\"value\":100}}",
        "{\"between\":{\"field\":\"preference.budget\",\"from\":99,\"to\":100}}",
        "{\"eq\":{\"field\":\"preference.vip\",\"value\":true}}",
        "{\"eq\":{\"field\":\"preference.vip\",\"value\":\"true\"}}",
        "{\"in\":{\"field\":\"preference.channel\",\"values\":[\"email\",\"push\"]}}",
        "{\"eq\":{\"field\":\"preference.birthday\",\"value\":\"1990-05-01\"}}",
        "{\"lt\":{\"field\":\"preference.birthday\",\"value\":\"2000-01-01\"}}",
        "{\"between\":{\"field\":\"preference.birthday\",\"from\":\"1990-01-01\",\"to\":\"2001-12-31\"}}",
        "{\"exists\":{\"field\":\"preference.vip\"}}",
        "{\"contains\":{\"field\":\"preference.channel\",\"value\":\"ms\"}}",
        "{\"eq\":{\"field\":\"consent.EMAIL\",\"value\":true}}",
        "{\"eq\":{\"field\":\"consent.EMAIL\",\"value\":false}}",
        "{\"eq\":{\"field\":\"consent.CRM_PROGRAM\",\"value\":true}}",
        "{\"in\":{\"field\":\"consent.SMS\",\"values\":[true,false]}}",
        "{\"subscribedTo\":{\"service\":\"newsletter\"}}",
        "{\"contains\":{\"field\":\"subscriptions\",\"value\":\"news\"}}",
        "{\"not\":{\"subscribedTo\":{\"service\":\"news\"}}}",
        "{\"gt\":{\"field\":\"registeredAt\",\"value\":\"2000-01-01\"}}",
        "{\"lt\":{\"field\":\"registeredAt\",\"value\":\"2000-01-01\"}}",
        "{\"and\":[{\"eq\":{\"field\":\"segment\",\"value\":\"S1\"}},{\"eq\":{\"field\":\"consent.EMAIL\",\"value\":true}}]}",
        "{\"or\":[{\"eq\":{\"field\":\"language\",\"value\":\"fr\"}},{\"gt\":{\"field\":\"preference.budget\",\"value\":120}}]}",
        "{\"not\":{\"or\":[{\"eq\":{\"field\":\"segment\",\"value\":\"S2\"}},{\"not\":{\"exists\":{\"field\":\"segment\"}}}]}}",
        "{\"and\":[{\"not\":{\"eq\":{\"field\":\"preference.vip\",\"value\":true}}},{\"exists\":{\"field\":\"preference.budget\"}}]}");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("rules")
  void sqlAndMemoryAgree(String json) {
    TenantContext.runAs(
        brand,
        () -> {
          Rule rule = engine.parse(json);
          Set<ConsumerId> inMemory =
              seed.stream().filter(id -> engine.evaluate(rule, id)).collect(Collectors.toSet());
          Set<ConsumerId> inSql = Set.copyOf(segmenter.matchingConsumers(rule));

          assertThat(inSql).as(json).isEqualTo(inMemory);
        });
  }

  @Test
  void seedIsDiverse() {
    TenantContext.runAs(
        brand,
        () -> {
          long matches =
              rules()
                  .map(engine::parse)
                  .filter(r -> !segmenter.matchingConsumers(r).isEmpty())
                  .count();
          assertThat(matches).as("la mayoría de reglas selecciona a alguien").isGreaterThan(25);
        });
  }

  @Test
  void untranslatableRuleIsRejected() {
    TenantContext.runAs(
        brand,
        () ->
            assertThatThrownBy(
                    () ->
                        segmenter.matchingConsumers(
                            engine.parse(
                                "{\"eventOccurred\":{\"type\":\"LOGIN\",\"within\":\"P1D\"}}")))
                .isInstanceOf(RuleNotTranslatableException.class));
  }
}
