package org.example.crm.rules.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.rules.infrastructure.JacksonRuleReader;

/** Consumidor de referencia y utilidades para los tests del DSL. */
public final class RuleFixtures {

  public static final Instant NOW = Instant.parse("2026-10-10T12:00:00Z");
  private static final JacksonRuleReader READER = new JacksonRuleReader();

  private RuleFixtures() {}

  public static Rule rule(String json) {
    return RuleParser.parse(READER.read(json));
  }

  public static Object tree(String json) {
    return READER.read(json);
  }

  /**
   * Consumidor de la Marca A, segmento S1, idioma es, alta 2026-01-15, ciudad "Ciudad Norte",
   * preferencias {@code budget=150.5, vip=true, birthday=1990-05-01, channel="email"}, opt-in de
   * programa y EMAIL, suscrito a {@code newsletter}, con un EMAIL_OPENED hace 1 día.
   */
  public static RuleContext reference() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("email", "ana@example.test");
    profile.put("language", "es");
    profile.put("segment", "S1");
    profile.put("status", "ACTIVE");
    profile.put("registeredAt", Instant.parse("2026-01-15T09:30:00Z"));
    return new RuleContext(
        ConsumerId.random(),
        profile,
        Map.of("city", "Ciudad Norte"),
        Map.of(
            "budget", PreferenceValue.of(new BigDecimal("150.5")),
            "vip", PreferenceValue.of(true),
            "birthday", PreferenceValue.of(LocalDate.of(1990, 5, 1)),
            "channel", PreferenceValue.of("email")),
        EnumSet.of(ConsentChannel.CRM_PROGRAM, ConsentChannel.EMAIL),
        Set.of("newsletter"),
        (type, since) ->
            type.equals("EMAIL_OPENED") && !NOW.minus(Duration.ofDays(1)).isBefore(since),
        NOW);
  }
}
