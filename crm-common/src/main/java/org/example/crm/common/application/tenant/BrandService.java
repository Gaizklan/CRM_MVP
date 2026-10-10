package org.example.crm.common.application.tenant;

import java.util.List;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.NotFoundException;

/** Casos de uso de gestión del catálogo de Marcas. */
public class BrandService {

  private final BrandRepository brands;

  public BrandService(BrandRepository brands) {
    this.brands = brands;
  }

  public List<Brand> list() {
    return brands.findAll();
  }

  public Brand get(BrandId id) {
    return brands
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Marca no encontrada: " + id));
  }

  public Brand create(String code, String name, String domain, String defaultLanguage) {
    Brand brand = Brand.create(code, name, domain, defaultLanguage);
    if (brands.findByCode(brand.code()).isPresent()) {
      throw new ConflictException("Ya existe una Marca con código " + brand.code());
    }
    ensureDomainFree(brand, null);
    return brands.save(brand);
  }

  public Brand update(
      BrandId id, String name, String domain, String defaultLanguage, boolean active) {
    Brand brand = get(id);
    brand.rename(name);
    brand.changeDomain(domain);
    brand.changeDefaultLanguage(defaultLanguage);
    if (active) {
      brand.activate();
    } else {
      brand.deactivate();
    }
    ensureDomainFree(brand, id);
    return brands.save(brand);
  }

  private void ensureDomainFree(Brand brand, BrandId self) {
    brand
        .domain()
        .flatMap(brands::findByDomain)
        .filter(other -> !other.id().equals(self))
        .ifPresent(
            other -> {
              throw new ConflictException(
                  "El dominio " + brand.domain().orElseThrow() + " ya pertenece a otra Marca");
            });
  }
}
