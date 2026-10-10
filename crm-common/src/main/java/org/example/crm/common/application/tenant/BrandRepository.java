package org.example.crm.common.application.tenant;

import java.util.List;
import org.example.crm.common.domain.Brand;

/** Puerto de salida: persistencia del catálogo de Marcas. */
public interface BrandRepository extends BrandLookup {

  List<Brand> findAll();

  Brand save(Brand brand);
}
