package org.example.crm.consumer.domain;

/** Canal o ámbito sujeto a consentimiento (requisitos §2.12). */
public enum ConsentChannel {
  EMAIL,
  SMS,
  MMS,
  PUSH,
  /** Servicios de suscripción del programa (boletines, novedades…). */
  SUBSCRIPTIONS,
  /** Participación en el programa CRM. Su opt-out implica el opt-out de todos los canales. */
  CRM_PROGRAM;

  public boolean isProgram() {
    return this == CRM_PROGRAM;
  }
}
