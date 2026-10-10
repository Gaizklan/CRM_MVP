package org.example.crm.consumer.application;

import java.util.Set;
import org.example.crm.consumer.domain.ConsumerId;

/**
 * Puerto de entrada de consulta de suscripciones para otros módulos, en particular el motor de
 * reglas (criterio {@code subscribedTo}). Opera en la Marca del contexto.
 */
public interface SubscriptionQuery {

  /** Códigos de los servicios a los que el consumidor está suscrito actualmente. */
  Set<String> activeServiceCodes(ConsumerId consumerId);

  default boolean isSubscribedTo(ConsumerId consumerId, String serviceCode) {
    return activeServiceCodes(consumerId).contains(serviceCode);
  }
}
