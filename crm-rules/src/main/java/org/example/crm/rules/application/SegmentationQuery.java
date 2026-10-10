package org.example.crm.rules.application;

import java.util.List;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.domain.ConsumerId;

/** Puerto de salida: ejecución de consultas de segmentación parametrizadas. */
public interface SegmentationQuery {

  List<ConsumerId> consumerIds(SqlFragment query);
}
