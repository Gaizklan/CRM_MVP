package org.example.crm.consumer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValueObjectsTest {

  @Test
  void emailIsNormalized() {
    assertThat(new Email("  Ana.Perez@Example.TEST ").value()).isEqualTo("ana.perez@example.test");
    assertThat(new Email("a@b.test")).hasToString("a@b.test");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "sin-arroba", "a@b", "a@@b.test", "a b@c.test"})
  void rejectsInvalidEmails(String value) {
    assertThatThrownBy(() -> new Email(value)).isInstanceOf(DomainException.class);
  }

  @Test
  void rejectsNullOrTooLongEmail() {
    assertThatThrownBy(() -> new Email(null)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new Email("a".repeat(250) + "@b.test"))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void phoneIsE164() {
    assertThat(new PhoneNumber("+34 600-000 000").value()).isEqualTo("+34600000000");
    assertThat(new PhoneNumber("+34600000000")).hasToString("+34600000000");
    assertThatThrownBy(() -> new PhoneNumber("600000000")).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new PhoneNumber(null)).isInstanceOf(DomainException.class);
  }

  @Test
  void languageTag() {
    assertThat(new LanguageTag("es-ES")).hasToString("es-ES");
    assertThatThrownBy(() -> new LanguageTag("ES")).isInstanceOf(DomainException.class);
  }

  @Test
  void consumerId() {
    UUID uuid = UUID.randomUUID();
    assertThat(ConsumerId.of(uuid.toString()).value()).isEqualTo(uuid);
    assertThat(ConsumerId.of(uuid.toString())).hasToString(uuid.toString());
    assertThatThrownBy(() -> ConsumerId.of("x")).isInstanceOf(DomainException.class);
  }

  @ParameterizedTest
  @CsvSource({
    "STRING, hola, hola",
    "NUMBER, 3.50, 3.5",
    "NUMBER, 100, 100",
    "BOOLEAN, TRUE, true",
    "BOOLEAN, false, false",
    "DATE, 2026-01-31, 2026-01-31"
  })
  void parsesTypedPreferenceValues(PreferenceType type, String text, String canonical) {
    PreferenceValue value = PreferenceValue.parse(type, text);

    assertThat(value.type()).isEqualTo(type);
    assertThat(value.asText()).isEqualTo(canonical);
    assertThat(PreferenceValue.parse(type, value.asText())).isEqualTo(value);
  }

  @ParameterizedTest
  @CsvSource({"NUMBER, abc", "BOOLEAN, yes", "DATE, 31/01/2026"})
  void rejectsMalformedPreferenceValues(PreferenceType type, String text) {
    assertThatThrownBy(() -> PreferenceValue.parse(type, text))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining(text);
  }

  @Test
  void preferenceValueRawAndValidation() {
    assertThat(PreferenceValue.of("x").raw()).isEqualTo("x");
    assertThat(PreferenceValue.of(new BigDecimal("2.0")).raw()).isEqualTo(new BigDecimal("2"));
    assertThat(PreferenceValue.of(true).raw()).isEqualTo(true);
    assertThat(PreferenceValue.of(LocalDate.of(2026, 1, 1)).raw())
        .isEqualTo(LocalDate.of(2026, 1, 1));
    assertThatThrownBy(() -> PreferenceValue.parse(PreferenceType.STRING, null))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> PreferenceValue.of("x".repeat(1001)))
        .isInstanceOf(DomainException.class);
  }
}
