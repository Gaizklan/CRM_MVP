package org.example.crm.common.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmPiiCipherTest {

  private static final SecureRandom RANDOM = new SecureRandom();

  static String key() {
    byte[] k = new byte[32];
    RANDOM.nextBytes(k);
    return Base64.getEncoder().encodeToString(k);
  }

  private final String k1 = key();
  private final String hmac = key();
  private final AesGcmPiiCipher cipher =
      new AesGcmPiiCipher(new PiiCryptoProperties("k1", k1, null, hmac));

  @Test
  void roundTripIsNonDeterministicAndTagged() {
    String a = cipher.encrypt("ana@example.test");
    String b = cipher.encrypt("ana@example.test");

    assertThat(a).startsWith("k1:").isNotEqualTo(b).doesNotContain("ana");
    assertThat(cipher.decrypt(a)).isEqualTo("ana@example.test");
    assertThat(cipher.decrypt(b)).isEqualTo("ana@example.test");
    assertThat(cipher.encrypt(null)).isNull();
    assertThat(cipher.decrypt(null)).isNull();
  }

  @Test
  void blindIndexIsDeterministicAndKeyed() {
    AesGcmPiiCipher otherHmac = new AesGcmPiiCipher(new PiiCryptoProperties("k1", k1, null, key()));

    assertThat(cipher.blindIndex("ana@example.test"))
        .isEqualTo(cipher.blindIndex("ana@example.test"))
        .hasSize(64)
        .isNotEqualTo(cipher.blindIndex("luis@example.test"))
        .isNotEqualTo(otherHmac.blindIndex("ana@example.test"));
    assertThat(cipher.blindIndex(null)).isNull();
  }

  @Test
  void tamperingIsDetected() {
    String c = cipher.encrypt("+34600000000");
    byte[] payload = Base64.getDecoder().decode(c.substring(3));
    payload[payload.length - 1] ^= 1;
    String tampered = "k1:" + Base64.getEncoder().encodeToString(payload);

    assertThatThrownBy(() -> cipher.decrypt(tampered)).hasMessageContaining("alterado");
    assertThatThrownBy(() -> cipher.decrypt("k1:AAAA")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> cipher.decrypt("sin-separador"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> cipher.decrypt("k9:" + c.substring(3)))
        .hasMessageContaining("desconocida");
  }

  @Test
  void keyIdIsAuthenticated() {
    AesGcmPiiCipher twoKeys =
        new AesGcmPiiCipher(new PiiCryptoProperties("k2", k1, "k1:" + k1, hmac));
    String c = cipher.encrypt("dato");

    // Misma clave material bajo otro id: el AAD no coincide y se rechaza.
    assertThatThrownBy(() -> twoKeys.decrypt("k2:" + c.substring(3)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(twoKeys.decrypt(c)).isEqualTo("dato");
  }

  @Test
  void rotationKeepsOldDataReadable() {
    String old = cipher.encrypt("ana@example.test");
    AesGcmPiiCipher rotated =
        new AesGcmPiiCipher(new PiiCryptoProperties("k2", key(), "k1:" + k1, hmac));

    assertThat(rotated.decrypt(old)).isEqualTo("ana@example.test");
    assertThat(rotated.needsReencryption(old)).isTrue();
    String fresh = rotated.encrypt("ana@example.test");
    assertThat(fresh).startsWith("k2:");
    assertThat(rotated.needsReencryption(fresh)).isFalse();
    assertThat(rotated.needsReencryption(null)).isFalse();
    assertThat(rotated.blindIndex("x")).isEqualTo(cipher.blindIndex("x"));
  }

  @Test
  void refusesMissingOrInvalidKeys() {
    assertThatThrownBy(() -> new AesGcmPiiCipher(new PiiCryptoProperties("k1", "", null, hmac)))
        .hasMessageContaining("CRM_PII_ENCRYPTION_KEY");
    assertThatThrownBy(
            () -> new AesGcmPiiCipher(new PiiCryptoProperties("k1", "no-base64!", null, hmac)))
        .hasMessageContaining("Base64");
    assertThatThrownBy(
            () ->
                new AesGcmPiiCipher(
                    new PiiCryptoProperties(
                        "k1", Base64.getEncoder().encodeToString(new byte[16]), null, hmac)))
        .hasMessageContaining("32 bytes");
    assertThatThrownBy(() -> new AesGcmPiiCipher(new PiiCryptoProperties("k 1", k1, null, hmac)))
        .hasMessageContaining("Identificador");
    assertThatThrownBy(
            () -> new AesGcmPiiCipher(new PiiCryptoProperties("k1", k1, "k1:" + k1, hmac)))
        .hasMessageContaining("duplicado");
    assertThatThrownBy(() -> new AesGcmPiiCipher(new PiiCryptoProperties("k1", k1, "k0", hmac)))
        .hasMessageContaining("formato");
    assertThatThrownBy(
            () ->
                new AesGcmPiiCipher(
                    new PiiCryptoProperties(
                        "k1", k1, null, Base64.getEncoder().encodeToString(new byte[8]))))
        .hasMessageContaining("al menos 32");
    assertThatThrownBy(() -> new AesGcmPiiCipher(new PiiCryptoProperties("k1", k1, null, null)))
        .hasMessageContaining("CRM_PII_BLIND_INDEX_KEY");
  }
}
