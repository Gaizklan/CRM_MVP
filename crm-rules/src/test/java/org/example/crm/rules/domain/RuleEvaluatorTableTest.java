package org.example.crm.rules.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tabla de casos de evaluación en memoria contra el consumidor de referencia. */
class RuleEvaluatorTableTest {

  @ParameterizedTest(name = "[{index}] {0} -> {1}")
  @CsvSource(
      delimiter = '|',
      quoteCharacter = '~',
      textBlock =
          """
{"eq":{"field":"segment","value":"S1"}}                                            | true
{"eq":{"field":"segment","value":"s1"}}                                            | false
{"ne":{"field":"segment","value":"S2"}}                                            | true
{"ne":{"field":"phone","value":"+34600000000"}}                                    | true
{"eq":{"field":"phone","value":"+34600000000"}}                                    | false
{"in":{"field":"language","values":["en","es"]}}                                   | true
{"in":{"field":"language","values":["en","fr"]}}                                   | false
{"gt":{"field":"preference.budget","value":100}}                                   | true
{"gt":{"field":"preference.budget","value":150.5}}                                 | false
{"lt":{"field":"preference.budget","value":150.50001}}                             | true
{"eq":{"field":"preference.budget","value":150.50}}                                | true
{"between":{"field":"preference.budget","from":150.5,"to":200}}                    | true
{"between":{"field":"preference.budget","from":0,"to":150}}                        | false
{"eq":{"field":"preference.vip","value":true}}                                     | true
{"eq":{"field":"preference.vip","value":"true"}}                                   | false
{"lt":{"field":"preference.birthday","value":"2000-01-01"}}                        | true
{"between":{"field":"preference.birthday","from":"1990-05-01","to":"1990-05-01"}}  | true
{"eq":{"field":"preference.birthday","value":"1990-05-01"}}                        | true
{"gt":{"field":"registeredAt","value":"2026-01-01"}}                               | true
{"lt":{"field":"registeredAt","value":"2026-01-15T09:30:00Z"}}                     | false
{"between":{"field":"registeredAt","from":"2026-01-15","to":"2026-01-16"}}         | true
{"exists":{"field":"attribute.city"}}                                              | true
{"exists":{"field":"attribute.zip"}}                                               | false
{"exists":{"field":"preference.unknown"}}                                          | false
{"contains":{"field":"attribute.city","value":"Norte"}}                            | true
{"contains":{"field":"email","value":"@example.test"}}                             | true
{"contains":{"field":"subscriptions","value":"newsletter"}}                        | true
{"contains":{"field":"subscriptions","value":"news"}}                              | false
{"subscribedTo":{"service":"newsletter"}}                                          | true
{"subscribedTo":{"service":"news"}}                                                | false
{"eq":{"field":"consent.EMAIL","value":true}}                                      | true
{"eq":{"field":"consent.SMS","value":true}}                                        | false
{"eventOccurred":{"type":"EMAIL_OPENED","within":"P2D"}}                           | true
{"eventOccurred":{"type":"EMAIL_OPENED","within":"PT12H"}}                         | false
{"eventOccurred":{"type":"EMAIL_CLICKED","within":"P30D"}}                         | false
{"gt":{"field":"preference.channel","value":1}}                                    | false
{"not":{"eq":{"field":"status","value":"WITHDRAWN"}}}                              | true
{"and":[{"eq":{"field":"segment","value":"S1"}},{"eq":{"field":"consent.EMAIL","value":true}}]} | true
{"and":[{"eq":{"field":"segment","value":"S1"}},{"eq":{"field":"consent.SMS","value":true}}]}   | false
{"or":[{"eq":{"field":"segment","value":"S2"}},{"subscribedTo":{"service":"newsletter"}}]}      | true
{"or":[{"eq":{"field":"segment","value":"S2"}},{"not":{"exists":{"field":"segment"}}}]}         | false
""")
  void evaluatesAgainstReferenceConsumer(String json, boolean expected) {
    assertThat(RuleEvaluator.evaluate(RuleFixtures.rule(json), RuleFixtures.reference()))
        .isEqualTo(expected);
  }
}
