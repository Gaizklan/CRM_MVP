package org.example.crm.common.infrastructure.persistence;

import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantResolverConfiguration {

  @Bean
  TenantResolver tenantResolver(BrandLookup brandLookup) {
    return new TenantResolver(brandLookup);
  }
}
