package org.example.crm.rules.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.rules.domain.RuleFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RuleSqlTranslatorTest {

  private final TestSqlCatalog catalog = new TestSqlCatalog();

  private SqlFragment sql(String json) {
    return RuleSqlTranslator.toCondition(RuleFixtures.rule(json), catalog);
  }

  @Test
  void valuesAreAlwaysParameters() {
    String evil = "S1') OR 1=1 --";
    SqlFragment f =
        sql("{\"eq\":{\"field\":\"segment\",\"value\":\"" + evil.replace("'", "'") + "\"}}");

    assertThat(f.sql()).isEqualTo("COALESCE(c.segment IN (?), FALSE)").doesNotContain("S1");
    assertThat(f.params()).containsExactly(evil);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"in\":{\"field\":\"language\",\"values\":[\"x' OR '1'='1\",\"es\"]}}",
        "{\"contains\":{\"field\":\"attribute.city\",\"value\":\"%'; DROP TABLE consumer; --\"}}",
        "{\"eq\":{\"field\":\"preference.note\",\"value\":\"'\\\"?\"}}",
        "{\"subscribedTo\":{\"service\":\"news-x\"}}",
        "{\"not\":{\"eq\":{\"field\":\"attribute.k\",\"value\":\"a'b\"}}}"
      })
  void noLiteralEverReachesSqlText(String json) {
    SqlFragment f = sql(json);
    Object tree = RuleFixtures.tree(json);

    assertThat(f.sql()).doesNotContain("'", "DROP", "news-x", "a'b", "OR '1'");
    assertThat(f.params()).isNotEmpty();
    assertThat(tree).isNotNull();
  }

  @Test
  void translatesEachOperator() {
    assertThat(sql("{\"ne\":{\"field\":\"segment\",\"value\":\"S1\"}}").sql())
        .isEqualTo("(NOT COALESCE(c.segment IN (?), FALSE))");
    assertThat(sql("{\"exists\":{\"field\":\"segment\"}}").sql())
        .isEqualTo("(c.segment IS NOT NULL)");
    assertThat(sql("{\"exists\":{\"field\":\"consent.SMS\"}}").sql()).isEqualTo("TRUE");
    assertThat(sql("{\"exists\":{\"field\":\"subscriptions\"}}").sql()).isEqualTo("TRUE");
    assertThat(sql("{\"exists\":{\"field\":\"attribute.city\"}}").sql())
        .isEqualTo("ATTR(? AND TRUE)");
    assertThat(sql("{\"exists\":{\"field\":\"preference.vip\"}}").params()).containsExactly("vip");
    assertThat(sql("{\"eq\":{\"field\":\"consent.SMS\",\"value\":false}}").sql())
        .isEqualTo("((NOT CONSENT(?)))");
    assertThat(sql("{\"contains\":{\"field\":\"subscriptions\",\"value\":\"news\"}}").sql())
        .isEqualTo("SUB(?)");
    assertThat(sql("{\"contains\":{\"field\":\"segment\",\"value\":\"S\"}}").sql())
        .isEqualTo("COALESCE(strpos(c.segment, ?) > 0, FALSE)");
    assertThat(sql("{\"contains\":{\"field\":\"preference.note\",\"value\":\"x\"}}").params())
        .containsExactly("note", "STRING", "x");
    assertThat(
            sql("{\"or\":[{\"eq\":{\"field\":\"segment\",\"value\":\"S1\"}},{\"subscribedTo\":{\"service\":\"news\"}}]}")
                .sql())
        .isEqualTo("(COALESCE(c.segment IN (?), FALSE) OR SUB(?))");
  }

  @Test
  void typedPreferenceComparisons() {
    SqlFragment number = sql("{\"gt\":{\"field\":\"preference.budget\",\"value\":100.5}}");
    assertThat(number.sql()).isEqualTo("PREF(? AND (p.type = ? AND p.number > ?))");
    assertThat(number.params()).containsExactly("budget", "NUMBER", new BigDecimal("100.5"));

    SqlFragment date =
        sql(
            "{\"between\":{\"field\":\"preference.birthday\",\"from\":\"1990-01-01\",\"to\":\"1990-12-31\"}}");
    assertThat(date.params())
        .containsExactly("birthday", "DATE", LocalDate.of(1990, 1, 1), LocalDate.of(1990, 12, 31));

    SqlFragment eqText = sql("{\"eq\":{\"field\":\"preference.d\",\"value\":\"2026-01-01\"}}");
    assertThat(eqText.sql()).contains("p.string = ?", "p.date = ?");

    SqlFragment bool = sql("{\"in\":{\"field\":\"preference.vip\",\"values\":[true]}}");
    assertThat(bool.params()).containsExactly("vip", "BOOLEAN", true);
    assertThat(sql("{\"lt\":{\"field\":\"preference.d\",\"value\":\"2026-01-01\"}}").params())
        .containsExactly("d", "DATE", LocalDate.of(2026, 1, 1));
  }

  @Test
  void registeredAtUsesTimestamps() {
    SqlFragment f =
        sql(
            "{\"between\":{\"field\":\"registeredAt\",\"from\":\"2026-01-01\",\"to\":\"2026-02-01T10:00:00Z\"}}");

    assertThat(f.params())
        .containsExactly(
            OffsetDateTime.parse("2026-01-01T00:00:00Z"),
            OffsetDateTime.parse("2026-02-01T10:00:00Z"));
    assertThat(sql("{\"lt\":{\"field\":\"registeredAt\",\"value\":\"2026-01-01\"}}").sql())
        .isEqualTo("COALESCE(c.registered_at < ?, FALSE)");
    assertThat(sql("{\"eq\":{\"field\":\"registeredAt\",\"value\":\"2026-01-01\"}}").params())
        .hasSize(1);
  }

  @Test
  void profileValuesCanBeTransformedByCatalog() {
    assertThat(sql("{\"eq\":{\"field\":\"email\",\"value\":\"a@b.test\"}}").params())
        .containsExactly("hash(a@b.test)");
  }

  @Test
  void untranslatableRulesAreRejectedExplicitly() {
    assertThatThrownBy(
            () ->
                sql(
                    "{\"and\":[{\"eventOccurred\":{\"type\":\"LOGIN\",\"within\":\"P1D\"}},{\"exists\":{\"field\":\"status\"}}]}"))
        .isInstanceOf(RuleNotTranslatableException.class)
        .hasMessageContaining("eventOccurred(LOGIN)")
        .hasMessageContaining("status")
        .satisfies(e -> assertThat(((RuleNotTranslatableException) e).reasons()).hasSize(2));
    assertThatThrownBy(() -> sql("{\"contains\":{\"field\":\"email\",\"value\":\"@x\"}}"))
        .isInstanceOf(RuleNotTranslatableException.class)
        .hasMessageContaining("no es posible en SQL");
    assertThatThrownBy(() -> sql("{\"eq\":{\"field\":\"phone\",\"value\":\"+34600000000\"}}"))
        .isInstanceOf(RuleNotTranslatableException.class)
        .hasMessageContaining("cifrado");
    assertThat(sql("{\"exists\":{\"field\":\"phone\"}}").sql())
        .isEqualTo("(c.phone_enc IS NOT NULL)");
  }

  @Test
  void fullQueryFiltersByBrand() {
    BrandId brand = BrandId.random();
    SqlFragment q =
        RuleSqlTranslator.toQuery(
            RuleFixtures.rule("{\"exists\":{\"field\":\"segment\"}}"), brand, catalog);

    assertThat(q.sql()).isEqualTo("SELECT id WHERE brand = ? AND (c.segment IS NOT NULL)");
    assertThat(q.params()).containsExactly(brand.value());
  }
}
