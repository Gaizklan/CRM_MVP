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
 * Solo tests: valores por defecto del entorno de pruebas.
 *
 * <ul>
 *   <li>Claves de cifrado de PII aleatorias generadas en memoria (una vez por JVM) si no se han
 *       definido por entorno. Nunca hay claves en el repositorio.
 *   <li>Relay del outbox sin planificación: los tests lo invocan explícitamente.
 * </ul>
 */
public class TestEnvironmentDefaults implements EnvironmentPostProcessor {

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
    Map<String, Object> defaults = new HashMap<>();
    String configured = env.getProperty("crm.crypto.pii.active-key");
    if (configured == null || configured.isBlank()) {
      defaults.putAll(KEYS);
    }
    if (env.getProperty("crm.outbox.relay.enabled") == null) {
      defaults.put("crm.outbox.relay.enabled", "false");
    }
    env.getPropertySources().addFirst(new MapPropertySource("test-defaults", defaults));
  }
}
