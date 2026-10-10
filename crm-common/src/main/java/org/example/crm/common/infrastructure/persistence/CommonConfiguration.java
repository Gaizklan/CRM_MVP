package org.example.crm.common.infrastructure.persistence;

import java.time.Clock;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.BrandRepository;
import org.example.crm.common.application.tenant.BrandService;
import org.example.crm.common.application.tenant.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

  @Bean
  TenantResolver tenantResolver(BrandLookup brandLookup) {
    return new TenantResolver(brandLookup);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  BrandService brandService(BrandRepository brandRepository) {
    return new BrandService(brandRepository);
  }
}
