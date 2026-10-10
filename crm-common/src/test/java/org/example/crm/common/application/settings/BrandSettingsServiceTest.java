package org.example.crm.common.application.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.crm.common.application.tenant.BrandServiceTest;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.common.domain.settings.BrandSettings;
import org.junit.jupiter.api.Test;

class BrandSettingsServiceTest {

  private final Map<BrandId, BrandSettings> stored = new HashMap<>();
  private final BrandSettingsRepository repository =
      new BrandSettingsRepository() {
        @Override
        public Optional<BrandSettings> find(BrandId brand) {
          return Optional.ofNullable(stored.get(brand));
        }

        @Override
        public void save(BrandId brand, BrandSettings settings, String updatedBy) {
          stored.put(brand, settings);
        }
      };
  private final BrandServiceTest.InMemoryBrandRepository brands =
      new BrandServiceTest.InMemoryBrandRepository();
  private final BrandSettingsService service = new BrandSettingsService(repository, brands);
  private final Brand brand = brands.save(Brand.create("marca-a", "Marca A", null, "es"));

  @Test
  void defaultsUntilCustomized() {
    assertThat(service.isCustomized(brand.id())).isFalse();
    assertThat(service.settings(brand.id())).isEqualTo(BrandSettings.defaults(brand));

    BrandSettings custom = withLanguages(List.of("es", "en"));
    service.update(brand.id(), custom, "admin:x");

    assertThat(service.isCustomized(brand.id())).isTrue();
    assertThat(service.settings(brand.id())).isEqualTo(custom);
    assertThat(service.defaults(brand.id())).isEqualTo(BrandSettings.defaults(brand));
  }

  @Test
  void languagesMustIncludeBrandDefault() {
    assertThatThrownBy(() -> service.update(brand.id(), withLanguages(List.of("en")), "admin:x"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("(es)");
  }

  @Test
  void unknownBrand() {
    assertThatThrownBy(() -> service.settings(BrandId.random()))
        .isInstanceOf(NotFoundException.class);
  }

  private BrandSettings withLanguages(List<String> languages) {
    BrandSettings d = BrandSettings.defaults(brand);
    return new BrandSettings(d.notifications(), languages, d.maxRetries(), 7, d.activeProviders());
  }
}
