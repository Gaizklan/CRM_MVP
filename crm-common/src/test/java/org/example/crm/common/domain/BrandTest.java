package org.example.crm.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BrandTest {

  @Test
  void createsActiveBrandWithNormalizedDomain() {
    Brand brand = Brand.create("marca-a", " Marca A ", "Marca-A.Example.test", "es");

    assertThat(brand.isActive()).isTrue();
    assertThat(brand.status()).isEqualTo(BrandStatus.ACTIVE);
    assertThat(brand.code()).isEqualTo("marca-a");
    assertThat(brand.name()).isEqualTo("Marca A");
    assertThat(brand.domain()).contains("marca-a.example.test");
    assertThat(brand.defaultLanguage()).isEqualTo("es");
    assertThat(brand.id()).isNotNull();
  }

  @Test
  void domainIsOptional() {
    Brand brand = Brand.create("marca-b", "Marca B", " ", "es-ES");

    assertThat(brand.domain()).isEmpty();
    assertThat(brand.servesHost("marca-b.example.test")).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"A", "Marca A", "-abc", "a", "abcdefghijklmnopqrstuvwxyz0123456789"})
  void rejectsInvalidCodes(String code) {
    assertThatThrownBy(() -> Brand.create(code, "Marca", null, "es"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("código de Marca");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void rejectsBlankNames(String name) {
    assertThatThrownBy(() -> Brand.create("marca-a", name, null, "es"))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void rejectsTooLongName() {
    assertThatThrownBy(() -> Brand.create("marca-a", "x".repeat(121), null, "es"))
        .isInstanceOf(DomainException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"bad domain", "-x.test", "x..test"})
  void rejectsInvalidDomains(String domain) {
    assertThatThrownBy(() -> Brand.create("marca-a", "Marca A", domain, "es"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Dominio");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ES", "spanish", "es_ES"})
  void rejectsInvalidLanguages(String language) {
    assertThatThrownBy(() -> Brand.create("marca-a", "Marca A", null, language))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Idioma");
  }

  @Test
  void servesHostIgnoringPortAndCase() {
    Brand brand = Brand.create("marca-a", "Marca A", "marca-a.example.test", "es");

    assertThat(brand.servesHost("MARCA-A.example.test:8080")).isTrue();
    assertThat(brand.servesHost("marca-a.example.test")).isTrue();
    assertThat(brand.servesHost("marca-b.example.test")).isFalse();
    assertThat(brand.servesHost(null)).isFalse();
  }

  @Test
  void mutatorsValidateAndApply() {
    Brand brand = Brand.create("marca-a", "Marca A", null, "es");

    brand.rename("Marca A2");
    brand.changeDomain("a2.example.test");
    brand.changeDefaultLanguage("en");
    brand.deactivate();

    assertThat(brand.name()).isEqualTo("Marca A2");
    assertThat(brand.domain()).contains("a2.example.test");
    assertThat(brand.defaultLanguage()).isEqualTo("en");
    assertThat(brand.isActive()).isFalse();
    brand.activate();
    assertThat(brand.isActive()).isTrue();
    assertThatThrownBy(() -> brand.rename("")).isInstanceOf(DomainException.class);
  }

  @Test
  void restoreKeepsIdentityAndEqualityIsById() {
    BrandId id = BrandId.random();
    Brand a = Brand.restore(id, "marca-a", "Marca A", null, "es", BrandStatus.INACTIVE);
    Brand b = Brand.restore(id, "marca-a", "Otro", null, "es", BrandStatus.ACTIVE);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(Brand.create("marca-a", "Marca A", null, "es"));
    assertThat(a.isActive()).isFalse();
  }
}
