package org.example.crm.common.infrastructure.persistence;

import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Conecta el {@link TenantContext} con el multi-tenancy por discriminador de Hibernate.
 *
 * <p>Sin Marca en contexto se devuelve {@link #NO_BRAND}, un identificador que no corresponde a
 * ninguna Marca: las consultas no devuelven datos y las inserciones fallan por clave foránea (fallo
 * cerrado). En ámbito raíz se devuelve {@link #ROOT}, que Hibernate trata como "sin filtro".
 */
@Component
public class BrandTenantIdentifierResolver
    implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

  static final UUID NO_BRAND = new UUID(0L, 0L);
  static final UUID ROOT = new UUID(0L, 1L);

  @Override
  public UUID resolveCurrentTenantIdentifier() {
    if (TenantContext.isRoot()) {
      return ROOT;
    }
    return TenantContext.current().map(BrandId::value).orElse(NO_BRAND);
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }

  @Override
  public boolean isRoot(UUID tenantId) {
    return ROOT.equals(tenantId);
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}
