package org.example.crm.common.infrastructure.crypto;

import org.example.crm.common.application.crypto.PiiCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PiiCryptoProperties.class)
public class CryptoConfiguration {

  /** Falla el arranque si faltan las claves o no son válidas. */
  @Bean
  PiiCipher piiCipher(PiiCryptoProperties properties) {
    return new AesGcmPiiCipher(properties);
  }
}
