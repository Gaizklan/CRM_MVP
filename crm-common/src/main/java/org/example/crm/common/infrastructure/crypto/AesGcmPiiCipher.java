package org.example.crm.common.infrastructure.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.example.crm.common.application.crypto.PiiCipher;

/**
 * AES-256-GCM con IV aleatorio de 96 bits y etiqueta de 128 bits. Formato: {@code
 * <idClave>:<base64(iv || cifrado+etiqueta)>}; el identificador de clave se autentica como AAD.
 * Índice ciego: HMAC-SHA256 en hexadecimal con una clave independiente.
 */
public class AesGcmPiiCipher implements PiiCipher {

  private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9_-]{1,16}");
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final String activeKeyId;
  private final Map<String, SecretKey> keys;
  private final SecretKey blindIndexKey;
  private final SecureRandom random = new SecureRandom();

  public AesGcmPiiCipher(PiiCryptoProperties properties) {
    this.activeKeyId = requireKeyId(properties.activeKeyId());
    Map<String, SecretKey> all = new LinkedHashMap<>();
    all.put(activeKeyId, aesKey(activeKeyId, properties.activeKey()));
    if (properties.previousKeys() != null && !properties.previousKeys().isBlank()) {
      for (String entry : properties.previousKeys().split(",")) {
        String[] parts = entry.strip().split(":", 2);
        if (parts.length != 2) {
          throw new IllegalStateException(
              "crm.crypto.pii.previous-keys debe tener el formato id:base64[,id:base64]");
        }
        String id = requireKeyId(parts[0]);
        if (all.containsKey(id)) {
          throw new IllegalStateException("Identificador de clave PII duplicado: " + id);
        }
        all.put(id, aesKey(id, parts[1]));
      }
    }
    this.keys = Map.copyOf(all);
    byte[] hmac = decodeKey("blind-index-key", properties.blindIndexKey());
    if (hmac.length < 32) {
      throw new IllegalStateException(
          "crm.crypto.pii.blind-index-key debe tener al menos 32 bytes");
    }
    this.blindIndexKey = new SecretKeySpec(hmac, "HmacSHA256");
  }

  @Override
  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(iv);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_BITS, iv));
      cipher.updateAAD(activeKeyId.getBytes(StandardCharsets.UTF_8));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] payload =
          ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
      return activeKeyId + ":" + Base64.getEncoder().encodeToString(payload);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Error cifrando PII", e);
    }
  }

  @Override
  public String decrypt(String ciphertext) {
    if (ciphertext == null) {
      return null;
    }
    int colon = ciphertext.indexOf(':');
    if (colon <= 0) {
      throw new IllegalStateException("Formato de PII cifrada no válido");
    }
    String keyId = ciphertext.substring(0, colon);
    SecretKey key = keys.get(keyId);
    if (key == null) {
      throw new IllegalStateException("Clave PII desconocida: " + keyId);
    }
    try {
      byte[] payload = Base64.getDecoder().decode(ciphertext.substring(colon + 1));
      if (payload.length < IV_BYTES + TAG_BITS / 8) {
        throw new IllegalStateException("PII cifrada truncada");
      }
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
      cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
      byte[] plain = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException(
          "No se pudo descifrar la PII (dato alterado o clave errónea)", e);
    }
  }

  @Override
  public String blindIndex(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(blindIndexKey);
      return HexFormat.of().formatHex(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Error calculando el índice ciego", e);
    }
  }

  @Override
  public boolean needsReencryption(String ciphertext) {
    return ciphertext != null && !ciphertext.startsWith(activeKeyId + ":");
  }

  private static String requireKeyId(String id) {
    if (id == null || !KEY_ID.matcher(id.strip()).matches()) {
      throw new IllegalStateException(
          "Identificador de clave PII no válido ([A-Za-z0-9_-], 1-16): " + id);
    }
    return id.strip();
  }

  private static SecretKey aesKey(String id, String base64) {
    byte[] bytes = decodeKey("clave " + id, base64);
    if (bytes.length != 32) {
      throw new IllegalStateException(
          "La clave PII " + id + " debe ser AES-256 (32 bytes en Base64)");
    }
    return new SecretKeySpec(bytes, "AES");
  }

  private static byte[] decodeKey(String name, String base64) {
    if (base64 == null || base64.isBlank()) {
      throw new IllegalStateException(
          "Falta la "
              + name
              + " de cifrado de PII. Defina CRM_PII_ENCRYPTION_KEY y"
              + " CRM_PII_BLIND_INDEX_KEY (p. ej. `openssl rand -base64 32`); ver ADR 0002.");
    }
    try {
      return Base64.getDecoder().decode(base64.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("La " + name + " de cifrado de PII no es Base64 válido");
    }
  }
}
