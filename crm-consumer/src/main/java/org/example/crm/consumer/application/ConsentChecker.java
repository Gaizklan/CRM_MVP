package org.example.crm.consumer.application;

import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsumerId;

/**
 * Puerto de entrada usado por mensajería (y cualquier módulo que actúe sobre un consumidor) para
 * verificar el opt-in del canal antes de cualquier acción. Opera en la Marca del contexto.
 */
public interface ConsentChecker {

  /**
   * {@code true} si el consumidor existe en la Marca actual, no está dado de baja y tiene opt-in
   * vigente del canal y del programa CRM.
   */
  boolean isAllowed(ConsumerId consumer, ConsentChannel channel);
}
