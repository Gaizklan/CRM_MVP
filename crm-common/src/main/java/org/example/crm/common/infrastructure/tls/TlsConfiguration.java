package org.example.crm.common.infrastructure.tls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TlsConfiguration {

  @Bean
  OutboundTlsPolicy outboundTlsPolicy(
      @Value("${crm.outbound-tls.allow-insecure-localhost:false}") boolean allowInsecureLocalhost) {
    return new OutboundTlsPolicy(allowInsecureLocalhost);
  }
}
