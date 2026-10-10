package org.example.crm.support;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Solo tests: genera en memoria claves de cifrado de PII aleatorias (una vez por JVM) si no se han
 * definido por entorno. Nunca hay claves en el repositorio.
 */
public class TestPiiKeys implements EnvironmentPostProcessor {

  private static final Map<String, Object> KEYS = generate();

  private static Map<String, Object> generate() {
    SecureRandom random = new SecureRandom();
    Map<String, Object> keys = new HashMap<>();
    keys.put("crm.crypto.pii.active-key-id", "test");
    keys.put("crm.crypto.pii.active-key", randomKey(random));
    keys.put("crm.crypto.pii.blind-index-key", randomKey(random));
    return Map.copyOf(keys);
  }

  private static String randomKey(SecureRandom random) {
    byte[] key = new byte[32];
    random.nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
    String configured = env.getProperty("crm.crypto.pii.active-key");
    if (configured == null || configured.isBlank()) {
      env.getPropertySources().addFirst(new MapPropertySource("test-pii-keys", KEYS));
    }
  }
}
