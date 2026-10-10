package org.example.crm.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.example.crm.common.application.crypto.PiiCipher;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.ProfileUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Email y teléfono se guardan cifrados; la búsqueda por email usa el índice ciego. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("dev")
class PiiEncryptionIT {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired ConsumerService consumers;
  @Autowired BrandLookup brands;
  @Autowired JdbcTemplate jdbc;
  @Autowired PiiCipher cipher;

  @Test
  void piiIsEncryptedAtRestAndSearchableByEmail() {
    BrandId brand = brands.findByCode("marca-a").orElseThrow().id();
    Consumer c =
        TenantContext.callAs(
            brand,
            () ->
                consumers.register(
                    new Email("Cifrado@Example.test"),
                    new PhoneNumber("+34611111111"),
                    new LanguageTag("es")));

    Map<String, Object> row =
        jdbc.queryForMap(
            "select email_ciphertext, email_hash, phone_ciphertext from consumer where id = ?",
            c.id().value());
    assertThat(row.toString()).doesNotContain("cifrado@example.test", "+34611111111");
    assertThat((String) row.get("email_ciphertext")).startsWith("test:");
    assertThat(cipher.decrypt((String) row.get("phone_ciphertext"))).isEqualTo("+34611111111");
    assertThat(row.get("email_hash")).isEqualTo(cipher.blindIndex("cifrado@example.test"));
    assertThat(
            jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'consumer'",
                String.class))
        .doesNotContain("email", "phone");

    TenantContext.runAs(
        brand,
        () -> {
          Consumer loaded = consumers.get(c.id());
          assertThat(loaded.email().value()).isEqualTo("cifrado@example.test");
          assertThat(loaded.phone()).contains(new PhoneNumber("+34611111111"));
          assertThatThrownBy(
                  () ->
                      consumers.register(
                          new Email("CIFRADO@example.test"), null, new LanguageTag("es")))
              .isInstanceOf(ConflictException.class);
          consumers.updateProfile(
              c.id(),
              new ProfileUpdate(
                  new Email("nuevo@example.test"), java.util.Optional.empty(), null, null, null));
          assertThat(consumers.get(c.id()).phone()).isEmpty();
          assertThat(consumers.get(c.id()).email().value()).isEqualTo("nuevo@example.test");
        });
  }
}
