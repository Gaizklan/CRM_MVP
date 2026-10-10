package org.example.crm.admin.application;

/** Puerto de salida: hash de contraseñas (BCrypt/Argon2). Nunca se almacenan en claro. */
public interface PasswordHasher {

  String hash(String rawPassword);
}
