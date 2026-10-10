package org.example.crm.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.infrastructure.persistence.JpaBrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Demuestra que la Marca A no ve datos de la Marca B (filtro de Marca en repositorios). */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("dev")
class BrandIsolationIT {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired JpaBrandRepository brands;
  @Autowired TenantProbeRepository probes;

  BrandId brandA;
  BrandId brandB;

  @BeforeEach
  void seed() {
    brandA = brands.findByCode("marca-a").map(Brand::id).orElseThrow();
    brandB = brands.findByCode("marca-b").map(Brand::id).orElseThrow();
    TenantContext.callAsRoot(
        () -> {
          probes.deleteAllInBatch();
          return null;
        });
  }

  @Test
  void devSeedContainsMarcaAAndMarcaB() {
    assertThat(brands.findAll()).extracting(Brand::name).contains("Marca A", "Marca B");
  }

  @Test
  void brandAOnlySeesItsOwnData() {
    TenantProbeEntity a1 =
        TenantContext.callAs(brandA, () -> probes.save(new TenantProbeEntity("a1")));
    TenantProbeEntity b1 =
        TenantContext.callAs(brandB, () -> probes.save(new TenantProbeEntity("b1")));

    TenantContext.runAs(
        brandA,
        () -> {
          assertThat(probes.findAll())
              .extracting(TenantProbeEntity::getLabel)
              .containsExactly("a1");
          assertThat(probes.findAllOrdered())
              .extracting(TenantProbeEntity::getLabel)
              .containsExactly("a1");
          assertThat(probes.findByLabel("b1")).isEmpty();
          assertThat(probes.findById(b1.getId())).isEmpty();
          assertThat(probes.existsById(b1.getId())).isFalse();
          assertThat(probes.findById(a1.getId())).isPresent();
          assertThat(probes.count()).isEqualTo(1);
        });
    TenantContext.runAs(
        brandB,
        () ->
            assertThat(probes.findAll())
                .extracting(TenantProbeEntity::getLabel)
                .containsExactly("b1"));
  }

  @Test
  void insertTakesBrandFromContext() {
    TenantProbeEntity saved =
        TenantContext.callAs(brandB, () -> probes.save(new TenantProbeEntity("x")));

    assertThat(saved.getBrandId()).isEqualTo(brandB);
  }

  @Test
  void withoutBrandNothingIsVisibleAndRootSeesAll() {
    TenantContext.runAs(brandA, () -> probes.save(new TenantProbeEntity("a1")));
    TenantContext.runAs(brandB, () -> probes.save(new TenantProbeEntity("b1")));

    assertThat(probes.findAll()).isEmpty();
    List<TenantProbeEntity> all = TenantContext.callAsRoot(probes::findAll);
    assertThat(all).hasSize(2);
  }

  @Test
  void cannotWriteIntoAnotherBrand() {
    assertThatThrownBy(
            () ->
                TenantContext.runAs(
                    brandA, () -> probes.save(TenantProbeEntity.forBrand(brandB, "intruso"))))
        .isInstanceOf(RuntimeException.class);
    List<TenantProbeEntity> all = TenantContext.callAsRoot(probes::findAll);
    assertThat(all).isEmpty();
  }
}
