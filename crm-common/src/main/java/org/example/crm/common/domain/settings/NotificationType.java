package org.example.crm.common.domain.settings;

/** Notificaciones transaccionales del programa con remitente y asunto configurables por Marca. */
public enum NotificationType {
  /** Verificación de email tras el registro (§4.2). */
  REGISTRATION_VERIFICATION,
  /** Enlace de restablecimiento de contraseña (§4.3). */
  PASSWORD_RESET,
  /** Aviso de cuenta bloqueada e instrucciones de desbloqueo (§4.8). */
  ACCOUNT_LOCKED,
  /** Confirmación de baja del programa (§4.4). */
  UNSUBSCRIBE_CONFIRMATION
}
