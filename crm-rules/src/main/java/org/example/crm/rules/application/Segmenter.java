package org.example.crm.rules.application;

import java.util.List;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.application.SegmentationSqlCatalog;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.rules.domain.Rule;

/**
 * Puerto de entrada de segmentación: selecciona en base de datos los consumidores de la Marca
 * actual que cumplen una regla, sin evaluarla en memoria.
 */
public class Segmenter {

  private final SegmentationSqlCatalog catalog;
  private final SegmentationQuery query;

  public Segmenter(SegmentationSqlCatalog catalog, SegmentationQuery query) {
    this.catalog = catalog;
    this.query = query;
  }

  /** SQL parametrizado de la regla en la Marca actual (para diagnóstico o materialización). */
  public SqlFragment sql(Rule rule) {
    return RuleSqlTranslator.toQuery(rule, TenantContext.require(), catalog);
  }

  /** Identificadores de los consumidores de la Marca actual que cumplen la regla. */
  public List<ConsumerId> matchingConsumers(Rule rule) {
    return query.consumerIds(sql(rule));
  }
}
