package org.example.crm.common.domain.settings;

/** Canal de entrega de comunicaciones con proveedor y reintentos configurables. */
public enum DeliveryChannel {
  EMAIL,
  SMS,
  MMS,
  PUSH,
  /** Buzón del portal (sin proveedor externo). */
  INBOX
}
