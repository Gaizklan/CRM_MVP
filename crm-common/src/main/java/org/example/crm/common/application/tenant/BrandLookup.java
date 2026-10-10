package org.example.crm.common.application.tenant;

import java.util.Optional;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;

/** Puerto de salida: consulta de Marcas para resolver el contexto de tenant. */
public interface BrandLookup {

  Optional<Brand> findById(BrandId id);

  Optional<Brand> findByCode(String code);

  Optional<Brand> findByDomain(String host);
}
