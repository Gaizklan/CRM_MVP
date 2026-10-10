package org.example.crm.common.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.NotFoundException;
import org.junit.jupiter.api.Test;

public class BrandServiceTest {

  public static final class InMemoryBrandRepository implements BrandRepository {
    final List<Brand> brands = new ArrayList<>();

    @Override
    public Optional<Brand> findById(BrandId id) {
      return brands.stream().filter(b -> b.id().equals(id)).findFirst();
    }

    @Override
    public Optional<Brand> findByCode(String code) {
      return brands.stream().filter(b -> b.code().equals(code)).findFirst();
    }

    @Override
    public Optional<Brand> findByDomain(String host) {
      return brands.stream().filter(b -> b.servesHost(host)).findFirst();
    }

    @Override
    public List<Brand> findAll() {
      return List.copyOf(brands);
    }

    @Override
    public Brand save(Brand brand) {
      brands.removeIf(b -> b.id().equals(brand.id()));
      brands.add(brand);
      return brand;
    }
  }

  private final InMemoryBrandRepository repository = new InMemoryBrandRepository();
  private final BrandService service = new BrandService(repository);

  @Test
  void createsAndLists() {
    Brand a = service.create("marca-a", "Marca A", "a.example.test", "es");

    assertThat(service.list()).containsExactly(a);
    assertThat(service.get(a.id())).isEqualTo(a);
  }

  @Test
  void rejectsDuplicateCodeAndDomain() {
    service.create("marca-a", "Marca A", "a.example.test", "es");

    assertThatThrownBy(() -> service.create("marca-a", "Otra", null, "es"))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.create("marca-b", "Marca B", "a.example.test", "es"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updatesFieldsAndStatus() {
    Brand a = service.create("marca-a", "Marca A", "a.example.test", "es");

    Brand updated = service.update(a.id(), "Marca A bis", "a.example.test", "en", false);

    assertThat(updated.name()).isEqualTo("Marca A bis");
    assertThat(updated.defaultLanguage()).isEqualTo("en");
    assertThat(updated.isActive()).isFalse();
    assertThat(service.update(a.id(), "Marca A", null, "es", true).isActive()).isTrue();
  }

  @Test
  void updateRejectsDomainOfAnotherBrand() {
    service.create("marca-a", "Marca A", "a.example.test", "es");
    Brand b = service.create("marca-b", "Marca B", null, "es");

    assertThatThrownBy(() -> service.update(b.id(), "Marca B", "a.example.test", "es", true))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void unknownBrandIsNotFound() {
    assertThatThrownBy(() -> service.get(BrandId.random())).isInstanceOf(NotFoundException.class);
  }
}
