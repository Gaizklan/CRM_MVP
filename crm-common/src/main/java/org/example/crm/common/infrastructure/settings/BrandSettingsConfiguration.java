package org.example.crm.common.infrastructure.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.crm.common.application.settings.BrandSettingsRepository;
import org.example.crm.common.application.settings.BrandSettingsService;
import org.example.crm.common.application.tenant.BrandLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class BrandSettingsConfiguration {

  @Bean
  BrandSettingsRepository brandSettingsRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    return new JdbcBrandSettingsRepository(jdbc, mapper);
  }

  @Bean
  BrandSettingsService brandSettingsService(
      BrandSettingsRepository repository, BrandLookup brands) {
    return new BrandSettingsService(repository, brands);
  }
}
