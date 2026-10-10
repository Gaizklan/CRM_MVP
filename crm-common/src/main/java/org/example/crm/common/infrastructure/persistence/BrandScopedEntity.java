package org.example.crm.common.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.example.crm.common.domain.BrandId;
import org.hibernate.annotations.TenantId;

/**
 * Superclase de toda entidad JPA de negocio: columna {@code brand_id} gestionada por Hibernate como
 * discriminador de tenant ({@link TenantId}).
 *
 * <ul>
 *   <li>Al insertar, {@code brand_id} toma la Marca del {@code TenantContext} actual.
 *   <li>Toda consulta (JPQL, Criteria, derivadas de Spring Data) añade {@code brand_id = :marca}.
 *   <li>Las búsquedas por id se filtran además en {@link TenantAwareJpaRepository}.
 * </ul>
 */
@MappedSuperclass
public abstract class BrandScopedEntity {

  @TenantId
  @Column(name = "brand_id", nullable = false, updatable = false)
  private UUID brandId;

  public BrandId getBrandId() {
    return brandId == null ? null : new BrandId(brandId);
  }

  /**
   * Asigna la Marca explícitamente. Solo es válido si coincide con la del contexto actual o en
   * ámbito raíz; en otro caso Hibernate rechaza la escritura.
   */
  protected void assignBrand(BrandId brand) {
    this.brandId = brand == null ? null : brand.value();
  }
}
