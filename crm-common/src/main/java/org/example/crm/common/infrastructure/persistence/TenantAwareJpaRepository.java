package org.example.crm.common.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Implementación base de todos los repositorios Spring Data de la plataforma. Además del filtro de
 * Hibernate (que aplica a consultas), garantiza que las búsquedas por id ({@code findById}, {@code
 * existsById}) no devuelvan entidades de otra Marca.
 */
public class TenantAwareJpaRepository<T, I> extends SimpleJpaRepository<T, I> {

  public TenantAwareJpaRepository(
      JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
  }

  @Override
  public Optional<T> findById(I id) {
    return super.findById(id).filter(TenantAwareJpaRepository::visibleInCurrentBrand);
  }

  @Override
  public boolean existsById(I id) {
    return findById(id).isPresent();
  }

  static boolean visibleInCurrentBrand(Object entity) {
    if (!(entity instanceof BrandScopedEntity scoped) || TenantContext.isRoot()) {
      return true;
    }
    BrandId brand = scoped.getBrandId();
    return TenantContext.current().map(current -> current.equals(brand)).orElse(false);
  }
}
