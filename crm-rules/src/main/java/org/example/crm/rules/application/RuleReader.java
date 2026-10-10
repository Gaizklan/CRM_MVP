package org.example.crm.rules.application;

/**
 * Puerto de salida: convierte el texto JSON de una regla en un árbol genérico Map/List/escalares.
 */
public interface RuleReader {

  /**
   * @throws org.example.crm.rules.domain.RuleValidationException si el texto no es JSON válido
   */
  Object read(String json);
}
