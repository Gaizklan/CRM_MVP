package org.example.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque de la plataforma CRM. Escanea todos los módulos bajo {@code org.example.crm}.
 */
@SpringBootApplication
public class CrmApplication {

  public static void main(String[] args) {
    SpringApplication.run(CrmApplication.class, args);
  }
}
