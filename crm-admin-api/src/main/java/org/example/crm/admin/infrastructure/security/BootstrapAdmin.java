package org.example.crm.admin.infrastructure.security;

import org.example.crm.admin.application.InternalUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN al arrancar si la tabla de usuarios internos está vacía y se han indicado
 * credenciales por entorno ({@code CRM_BOOTSTRAP_ADMIN_USERNAME}, {@code
 * CRM_BOOTSTRAP_ADMIN_PASSWORD}). Nunca hay credenciales en el repositorio.
 */
@Component
class BootstrapAdmin implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

  private final InternalUserService service;
  private final String username;
  private final String password;

  BootstrapAdmin(
      InternalUserService service,
      @Value("${crm.bootstrap-admin.username:}") String username,
      @Value("${crm.bootstrap-admin.password:}") String password) {
    this.service = service;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (username.isBlank() || password.isBlank()) {
      return;
    }
    if (service.bootstrapAdmin(username, password)) {
      log.info("Creado el usuario ADMIN inicial '{}'", username);
    }
  }
}
