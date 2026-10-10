package org.example.crm.tracking.domain;

import java.util.Arrays;
import java.util.Optional;

/** Tipos de evento de usuario predefinidos (requisitos §2.2). */
public enum UserEventType {
  LOGIN,
  LOGOUT,
  /** Clic en un servicio del portal. */
  SERVICE_CLICK,
  /** Cambio de experiencia de usuario (interfaz, tema, idioma del portal…). */
  EXPERIENCE_CHANGED,
  EMAIL_SENT,
  EMAIL_OPENED,
  /** Clic en un contenido de un email. */
  EMAIL_CLICKED,
  /** Cambio de preferencias o consentimientos. */
  PREFERENCES_CHANGED;

  public static Optional<UserEventType> parse(String name) {
    return Arrays.stream(values()).filter(t -> t.name().equals(name)).findFirst();
  }
}
