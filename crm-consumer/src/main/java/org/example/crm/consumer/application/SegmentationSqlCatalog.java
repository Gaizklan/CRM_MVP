package org.example.crm.consumer.application;

import java.util.Optional;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.PreferenceType;

/**
 * Contrato de consulta SQL publicado por el módulo de consumidores para la segmentación. Es la
 * única vía por la que otros módulos (motor de reglas) consultan sus tablas: este módulo conoce el
 * esquema y entrega fragmentos parametrizados; quien los compone nunca concatena valores.
 *
 * <p>Todos los fragmentos se refieren al consumidor de la consulta principal ({@link
 * #selectConsumerIds}).
 */
public interface SegmentationSqlCatalog {

  /** Columna de un campo de perfil y sus capacidades. */
  record ProfileColumn(String expression, boolean substringSearch) {}

  /**
   * Columna SQL de un campo de perfil ({@code email, phone, language, segment, status,
   * registeredAt}); vacío si no es consultable en SQL.
   */
  Optional<ProfileColumn> profileColumn(String field);

  /**
   * Valor a enlazar para comparar por igualdad con un campo de perfil (permite transformaciones
   * como índices ciegos de columnas cifradas).
   */
  default Object bindProfileValue(String field, Object value) {
    return value;
  }

  /** Expresión del valor de atributo dentro de {@link #attributeExists}. */
  String attributeValueColumn();

  /** {@code EXISTS} de un atributo con ese nombre que cumple la condición. */
  SqlFragment attributeExists(String name, SqlFragment condition);

  /** Expresión de la columna con el tipo de la preferencia dentro de {@link #preferenceExists}. */
  String preferenceTypeColumn();

  /** Expresión de la columna tipada de preferencias dentro de {@link #preferenceExists}. */
  String preferenceColumn(PreferenceType type);

  /** {@code EXISTS} de una preferencia con esa clave que cumple la condición. */
  SqlFragment preferenceExists(String key, SqlFragment condition);

  /** Canal permitido: opt-in vigente del canal y del programa CRM. */
  SqlFragment consentAllowed(ConsentChannel channel);

  /** Suscripción activa al servicio con ese código. */
  SqlFragment subscribedTo(String serviceCode);

  /** Consulta de identificadores de consumidores de la Marca que cumplen {@code where}. */
  SqlFragment selectConsumerIds(BrandId brand, SqlFragment where);
}
