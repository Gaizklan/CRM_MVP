package org.example.crm.common.infrastructure.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Claves de cifrado de PII, siempre desde configuración externa (variables de entorno o gestor de
 * secretos), nunca en el repositorio.
 *
 * @param activeKeyId identificador de la clave activa (p. ej. {@code k2})
 * @param activeKey clave AES-256 activa en Base64 (32 bytes)
 * @param previousKeys claves anteriores aún válidas para descifrar: {@code id:base64,id:base64}
 * @param blindIndexKey clave HMAC-SHA256 del índice ciego en Base64 (≥ 32 bytes)
 */
@ConfigurationProperties("crm.crypto.pii")
public record PiiCryptoProperties(
    String activeKeyId, String activeKey, String previousKeys, String blindIndexKey) {}
