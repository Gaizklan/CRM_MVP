package org.example.crm.rules.infrastructure;

import java.util.List;
import java.util.UUID;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.rules.application.SegmentationQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Ejecuta consultas de segmentación con parámetros enlazados ({@code PreparedStatement}). */
@Component
class JdbcSegmentationQuery implements SegmentationQuery {

  private final JdbcTemplate jdbc;

  JdbcSegmentationQuery(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<ConsumerId> consumerIds(SqlFragment query) {
    return jdbc.query(
        query.sql(),
        (rs, i) -> new ConsumerId(rs.getObject(1, UUID.class)),
        query.params().toArray());
  }
}
