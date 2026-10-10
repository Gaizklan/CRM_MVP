package org.example.crm.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.example.crm.app.migration.V8__EncryptConsumerPii;
import org.example.crm.common.infrastructure.crypto.AesGcmPiiCipher;
import org.example.crm.common.infrastructure.crypto.PiiCryptoProperties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** La migración V7–V9 cifra los datos personales existentes en claro. */
@Testcontainers(disabledWithoutDocker = true)
class PiiMigrationIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static String key() {
    byte[] k = new byte[32];
    new SecureRandom().nextBytes(k);
    return Base64.getEncoder().encodeToString(k);
  }

  @Test
  void encryptsExistingPlaintextPii() {
    DriverManagerDataSource ds =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    JdbcTemplate jdbc = new JdbcTemplate(ds);
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .target("6")
        .load()
        .migrate();
    UUID brand = UUID.randomUUID();
    UUID consumer = UUID.randomUUID();
    jdbc.update(
        "insert into brand (id, code, name, default_language, status) values (?, 'mig', 'Marca A',"
            + " 'es', 'ACTIVE')",
        brand);
    jdbc.update(
        "insert into consumer (id, brand_id, email, phone, language, status, registered_at)"
            + " values (?, ?, 'antiguo@example.test', '+34600000009', 'es', 'ACTIVE', now())",
        consumer,
        brand);

    AesGcmPiiCipher cipher = new AesGcmPiiCipher(new PiiCryptoProperties("k1", key(), null, key()));
    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .javaMigrations(new V8__EncryptConsumerPii(cipher))
        .load()
        .migrate();

    Map<String, Object> row = jdbc.queryForMap("select * from consumer where id = ?", consumer);
    assertThat(row).doesNotContainKeys("email", "phone");
    assertThat(cipher.decrypt((String) row.get("email_ciphertext")))
        .isEqualTo("antiguo@example.test");
    assertThat(cipher.decrypt((String) row.get("phone_ciphertext"))).isEqualTo("+34600000009");
    assertThat(row.get("email_hash")).isEqualTo(cipher.blindIndex("antiguo@example.test"));
  }
}
