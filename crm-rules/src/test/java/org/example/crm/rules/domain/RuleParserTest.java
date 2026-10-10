package org.example.crm.rules.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Validación de reglas con mensajes claros (ruta + motivo). */
class RuleParserTest {

  @ParameterizedTest(name = "[{index}] {0}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '~',
      textBlock =
          """
[]                                                              | $: se esperaba un objeto con un operador
{}                                                              | $: cada nodo debe tener exactamente un operador
{"eq":{"field":"segment","value":"S1"},"ne":{"field":"segment","value":"S1"}} | exactamente un operador
{"like":{"field":"segment","value":"S"}}                        | $: operador desconocido 'like'
{"and":[]}                                                      | $.and: se esperaba una lista no vacía
{"and":{"eq":{}}}                                               | $.and: se esperaba una lista no vacía
{"eq":{"field":"segment"}}                                      | $.eq: faltan las propiedades [value]
{"eq":{"field":"segment","value":"S1","extra":1}}               | $.eq: propiedades no reconocidas [extra]
{"eq":{"field":"edad","value":30}}                              | $.eq.field: campo desconocido 'edad'
{"eq":{"field":"consent.FAX","value":true}}                     | $.eq.field: campo desconocido 'consent.FAX'
{"eq":{"field":"segment","value":null}}                         | $.eq.value: se esperaba un literal
{"eq":{"field":"segment","value":[1]}}                          | $.eq.value: se esperaba un literal
{"eq":{"field":"segment","value":1}}                            | el campo 'segment' requiere un texto
{"eq":{"field":"consent.EMAIL","value":"yes"}}                  | el campo 'consent.EMAIL' requiere un booleano
{"gt":{"field":"segment","value":"S1"}}                         | gt/lt/between no aplican
{"gt":{"field":"registeredAt","value":"ayer"}}                  | requiere una fecha yyyy-MM-dd o un instante
{"gt":{"field":"preference.budget","value":"mucho"}}            | requiere un número o una fecha
{"eq":{"field":"subscriptions","value":"news"}}                 | no admite eq/ne/in/gt/lt
{"in":{"field":"segment","values":[]}}                          | $.in.values: se esperaba una lista de 1 a 1000
{"in":{"field":"segment","values":["S1",2]}}                    | $.in.values[1]: el campo 'segment' requiere un texto
{"between":{"field":"preference.budget","from":1,"to":"2026-01-01"}} | $.between: 'from' y 'to' deben ser del mismo tipo
{"contains":{"field":"consent.EMAIL","value":"x"}}              | 'contains' solo se aplica
{"contains":{"field":"segment","value":""}}                     | 'contains' requiere un texto no vacío
{"subscribedTo":{"service":"Bad Code"}}                         | $.subscribedTo.service: se esperaba el código
{"eventOccurred":{"type":"opened","within":"P1D"}}              | $.eventOccurred.type: se esperaba un tipo de evento
{"eventOccurred":{"type":"EMAIL_OPENED","within":"2 days"}}     | $.eventOccurred.within: se esperaba una duración
{"eventOccurred":{"type":"EMAIL_OPENED","within":"P400D"}}      | hasta 366 días
{"not":{"or":[{"eq":{"field":"x","value":1}}]}}                 | $.not.or[0].eq.field: campo desconocido
""")
  void reportsClearErrors(String json, String expectedMessage) {
    assertThatThrownBy(() -> RuleFixtures.rule(json))
        .isInstanceOf(RuleValidationException.class)
        .satisfies(
            e ->
                assertThat(((RuleValidationException) e).errors())
                    .anyMatch(m -> m.contains(expectedMessage)));
  }

  @Test
  void collectsAllErrors() {
    List<String> errors =
        RuleParser.validate(
            RuleFixtures.tree(
                """
{"and":[{"eq":{"field":"x","value":1}},{"gt":{"field":"segment","value":1}},{"foo":1}]}
"""));

    assertThat(errors).hasSize(3);
    assertThat(errors.get(0)).startsWith("$.and[0].eq.field");
    assertThat(errors.get(1)).startsWith("$.and[1].gt.value");
    assertThat(errors.get(2)).startsWith("$.and[2]");
  }

  @Test
  void validRuleHasNoErrors() {
    assertThat(RuleParser.validate(RuleFixtures.tree("{\"exists\":{\"field\":\"segment\"}}")))
        .isEmpty();
  }

  @Test
  void enforcesDepthAndSizeLimits() {
    String deep = "{\"exists\":{\"field\":\"segment\"}}";
    for (int i = 0; i < RuleParser.MAX_DEPTH; i++) {
      deep = "{\"not\":" + deep + "}";
    }
    String finalDeep = deep;
    assertThatThrownBy(() -> RuleFixtures.rule(finalDeep)).hasMessageContaining("anidamiento");

    StringBuilder wide = new StringBuilder("{\"or\":[");
    for (int i = 0; i < RuleParser.MAX_NODES; i++) {
      wide.append(i == 0 ? "" : ",").append("{\"exists\":{\"field\":\"segment\"}}");
    }
    wide.append("]}");
    assertThatThrownBy(() -> RuleFixtures.rule(wide.toString())).hasMessageContaining("nodos");
  }

  @Test
  void buildsExpectedTree() {
    Rule rule =
        RuleFixtures.rule(
            """
            {"and":[
              {"in":{"field":"segment","values":["S1","S2"]}},
              {"eventOccurred":{"type":"LOGIN","within":"P7D"}},
              {"between":{"field":"preference.age","from":18,"to":30}}
            ]}
            """);

    assertThat(rule).isInstanceOf(Rule.And.class);
    List<Rule> children = ((Rule.And) rule).rules();
    assertThat(children.get(0)).isInstanceOf(Rule.In.class);
    assertThat(children.get(1)).isEqualTo(new Rule.EventOccurred("LOGIN", Duration.ofDays(7)));
    Rule.Between between = (Rule.Between) children.get(2);
    assertThat(between.field().kind()).isEqualTo(FieldRef.Kind.PREFERENCE);
    assertThat(between.from().raw()).isEqualTo(new java.math.BigDecimal("18"));
    assertThat(Rule.Op.GT.json()).isEqualTo("gt");
  }

  @Test
  void acceptsJavaNumbersAndBooleansFromAnyJsonParser() {
    Rule rule = RuleParser.parse(Map.of("gt", Map.of("field", "preference.n", "value", 3)));
    assertThat(((Rule.Compare) rule).value())
        .isEqualTo(new Literal.Number(new java.math.BigDecimal("3")));
    Rule bool = RuleParser.parse(Map.of("eq", Map.of("field", "preference.b", "value", false)));
    assertThat(((Rule.Compare) bool).value().raw()).isEqualTo(false);
    assertThat(new Literal.Number(new java.math.BigDecimal("0.00")).raw())
        .isEqualTo(java.math.BigDecimal.ZERO);
  }

  @Test
  void invalidJsonIsReported() {
    assertThatThrownBy(() -> RuleFixtures.tree("{\"eq\":"))
        .isInstanceOf(RuleValidationException.class)
        .hasMessageContaining("JSON no válido");
    assertThatThrownBy(() -> RuleFixtures.tree(" ")).hasMessageContaining("vacía");
  }

  @Test
  void fieldCatalog() {
    assertThat(FieldRef.parse("attribute.1x")).isEmpty();
    assertThat(FieldRef.parse("preference.")).isEmpty();
    assertThat(FieldRef.parse("unknown.x")).isEmpty();
    assertThat(FieldRef.parse(null)).isEmpty();
    assertThat(FieldRef.parse("consent.PUSH"))
        .get()
        .extracting(FieldRef::type)
        .isEqualTo(ValueType.BOOLEAN);
    assertThat(FieldRef.describeValidFields()).contains("subscriptions", "consent.<EMAIL|");
  }

  @Test
  void temporalParsing() {
    assertThat(Temporal.date("2026-02-30")).isEmpty();
    assertThat(Temporal.date("26-01-01")).isEmpty();
    assertThat(Temporal.instant("2026-01-01")).isPresent();
    assertThat(Temporal.instant("2026-01-01T00:00:00Z")).isPresent();
    assertThat(Temporal.instant("x")).isEmpty();
    assertThat(Temporal.instant(null)).isEmpty();
  }

  @Test
  void contextWithoutEvents() {
    RuleContext ctx = RuleFixtures.reference();
    RuleContext noEvents =
        new RuleContext(
            ctx.consumerId(),
            ctx.profile(),
            ctx.attributes(),
            ctx.preferences(),
            ctx.allowedChannels(),
            ctx.subscriptions(),
            RuleContext.RecentEvents.none(),
            ctx.now());

    assertThat(
            RuleEvaluator.evaluate(
                RuleFixtures.rule(
                    "{\"eventOccurred\":{\"type\":\"EMAIL_OPENED\",\"within\":\"P2D\"}}"),
                noEvents))
        .isFalse();
  }
}
