package org.example.crm.tenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;

/** Entidad de prueba con Marca. */
@Entity
@Table(name = "test_tenant_probe")
public class TenantProbeEntity extends BrandScopedEntity {

  @Id private UUID id;

  private String label;

  protected TenantProbeEntity() {}

  public TenantProbeEntity(String label) {
    this.id = UUID.randomUUID();
    this.label = label;
  }

  public static TenantProbeEntity forBrand(BrandId brand, String label) {
    TenantProbeEntity entity = new TenantProbeEntity(label);
    entity.assignBrand(brand);
    return entity;
  }

  public UUID getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }
}
