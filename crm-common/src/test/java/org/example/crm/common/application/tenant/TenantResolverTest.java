package org.example.crm.common.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.junit.jupiter.api.Test;

public class TenantResolverTest {

  public static final Brand A = Brand.create("marca-a", "Marca A", "marca-a.example.test", "es");
  public static final Brand B = Brand.create("marca-b", "Marca B", "marca-b.example.test", "es");
  static final Brand OFF = Brand.create("marca-off", "Marca inactiva", "off.example.test", "es");

  static {
    OFF.deactivate();
  }

  /** Implementación en memoria del puerto. */
  public static final class InMemoryBrands implements BrandLookup {
    private final List<Brand> brands = List.of(A, B, OFF);

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
  }

  private final TenantResolver resolver = new TenantResolver(new InMemoryBrands());

  @Test
  void portalResolvesByPathPrefix() {
    assertThat(resolver.resolvePortal("localhost", "/portal/marca-b/home")).contains(B.id());
    assertThat(resolver.resolvePortal("marca-a.example.test", "/portal/MARCA-B")).contains(B.id());
  }

  @Test
  void portalResolvesByDomainWhenNoPrefix() {
    assertThat(resolver.resolvePortal("marca-a.example.test:8443", "/home")).contains(A.id());
    assertThat(resolver.resolvePortal("unknown.example.test", "/home")).isEmpty();
    assertThat(resolver.resolvePortal(null, "/home")).isEmpty();
  }

  @Test
  void portalIgnoresInactiveBrands() {
    assertThat(resolver.resolvePortal("off.example.test", "/")).isEmpty();
    assertThatThrownBy(() -> resolver.resolvePortal("localhost", "/portal/marca-off/"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void portalUnknownPrefixIsNotFound() {
    assertThatThrownBy(() -> resolver.resolvePortal("localhost", "/portal/nope/x"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void adminResolvesHeaderBeforeSelector() {
    assertThat(resolver.resolveAdmin("marca-a", "marca-b")).contains(A.id());
    assertThat(resolver.resolveAdmin(" ", "marca-b")).contains(B.id());
    assertThat(resolver.resolveAdmin(null, "MARCA-B")).contains(B.id());
    assertThat(resolver.resolveAdmin(B.id().toString(), null)).contains(B.id());
  }

  @Test
  void adminWithoutSelectionIsEmpty() {
    assertThat(resolver.resolveAdmin(null, null)).isEmpty();
    assertThat(resolver.resolveAdmin("", "")).isEmpty();
  }

  @Test
  void adminUnknownBrandIsError() {
    assertThatThrownBy(() -> resolver.resolveAdmin("marca-z", null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("marca-z");
  }

  @Test
  void extractsCodeFromPortalPath() {
    assertThat(TenantResolver.brandCodeFromPortalPath("/portal/")).isEmpty();
    assertThat(TenantResolver.brandCodeFromPortalPath("/other")).isEmpty();
    assertThat(TenantResolver.brandCodeFromPortalPath(null)).isEmpty();
    assertThat(TenantResolver.brandCodeFromPortalPath("/portal/marca-a")).contains("marca-a");
  }
}
