package org.example.crm.common.application.settings;

import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.common.domain.settings.BrandSettings;

/**
 * Configuración por Marca. Se lee de base de datos en cada consulta, de modo que los cambios
 * aplican sin redespliegue; una Marca sin ajustes guardados usa {@link BrandSettings#defaults}.
 */
public class BrandSettingsService {

  private final BrandSettingsRepository repository;
  private final BrandLookup brands;

  public BrandSettingsService(BrandSettingsRepository repository, BrandLookup brands) {
    this.repository = repository;
    this.brands = brands;
  }

  /** Ajustes efectivos de la Marca (guardados o por defecto). */
  public BrandSettings settings(BrandId brandId) {
    return repository.find(brandId).orElseGet(() -> BrandSettings.defaults(brand(brandId)));
  }

  /** {@code true} si la Marca tiene ajustes guardados (no solo los valores por defecto). */
  public boolean isCustomized(BrandId brandId) {
    brand(brandId);
    return repository.find(brandId).isPresent();
  }

  public BrandSettings defaults(BrandId brandId) {
    return BrandSettings.defaults(brand(brandId));
  }

  /** Sustituye los ajustes de la Marca. El idioma por defecto debe estar entre los habilitados. */
  public BrandSettings update(BrandId brandId, BrandSettings settings, String updatedBy) {
    Brand brand = brand(brandId);
    if (!settings.languages().contains(brand.defaultLanguage())) {
      throw new DomainException(
          "Los idiomas deben incluir el idioma por defecto de la Marca ("
              + brand.defaultLanguage()
              + ")");
    }
    repository.save(brandId, settings, updatedBy);
    return settings;
  }

  private Brand brand(BrandId id) {
    return brands
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Marca no encontrada: " + id));
  }
}
