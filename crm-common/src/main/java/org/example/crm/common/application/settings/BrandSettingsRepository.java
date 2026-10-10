package org.example.crm.common.application.settings;

import java.util.Optional;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.settings.BrandSettings;

/** Puerto de salida: ajustes guardados por Marca. */
public interface BrandSettingsRepository {

  Optional<BrandSettings> find(BrandId brand);

  void save(BrandId brand, BrandSettings settings, String updatedBy);
}
