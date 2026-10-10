package org.example.crm.common.infrastructure.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.example.crm.common.application.settings.BrandSettingsRepository;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.settings.BrandSettings;
import org.springframework.jdbc.core.JdbcTemplate;

/** Ajustes por Marca como documento JSON (tabla {@code brand_settings}). */
public class JdbcBrandSettingsRepository implements BrandSettingsRepository {

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public JdbcBrandSettingsRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public Optional<BrandSettings> find(BrandId brand) {
    List<String> rows =
        jdbc.queryForList(
            "SELECT settings::text FROM brand_settings WHERE brand_id = ?",
            String.class,
            brand.value());
    return rows.stream().findFirst().map(this::read);
  }

  @Override
  public void save(BrandId brand, BrandSettings settings, String updatedBy) {
    jdbc.update(
        "INSERT INTO brand_settings (brand_id, settings, updated_at, updated_by)"
            + " VALUES (?, ?::jsonb, now(), ?) ON CONFLICT (brand_id) DO UPDATE"
            + " SET settings = EXCLUDED.settings, updated_at = now(), updated_by ="
            + " EXCLUDED.updated_by",
        brand.value(),
        write(settings),
        updatedBy);
  }

  private BrandSettings read(String json) {
    try {
      return mapper.readValue(json, BrandSettings.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Ajustes de Marca almacenados no válidos", e);
    }
  }

  private String write(BrandSettings settings) {
    try {
      return mapper.writeValueAsString(settings);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
