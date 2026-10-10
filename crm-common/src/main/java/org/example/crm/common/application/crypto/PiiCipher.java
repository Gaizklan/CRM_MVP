package org.example.crm.common.application.crypto;

/**
 * Cifrado de datos personales en reposo (email, teléfono) e índice ciego para búsquedas por
 * igualdad. Las claves son externas a la aplicación (ver ADR 0002).
 */
public interface PiiCipher {

  /**
   * Cifra con la clave activa. El resultado incluye el identificador de clave y es no determinista.
   */
  String encrypt(String plaintext);

  /** Descifra con la clave indicada en el propio texto cifrado (admite claves anteriores). */
  String decrypt(String ciphertext);

  /**
   * Índice ciego determinista (HMAC) del valor, para búsquedas e índices únicos sin descifrar. El
   * llamante debe pasar el valor ya normalizado.
   */
  String blindIndex(String plaintext);

  /** {@code true} si el texto cifrado no usa la clave activa (pendiente de recifrar tras rotar). */
  boolean needsReencryption(String ciphertext);
}
