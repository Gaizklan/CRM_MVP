package org.example.crm.tracking.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.tracking.application.UserEventRepository;
import org.example.crm.tracking.domain.UserEvent;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Eventos de usuario con JDBC (tabla de solo inserción de gran volumen). Toda consulta filtra
 * explícitamente por la Marca del contexto (las consultas nativas no pasan por el filtro de
 * Hibernate, ver ADR 0001).
 */
public class JdbcUserEventRepository implements UserEventRepository {

  private static final TypeReference<Map<String, String>> ATTRIBUTES = new TypeReference<>() {};

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public JdbcUserEventRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  @Override
  public void save(UserEvent event) {
    if (!event.brandId().equals(TenantContext.require())) {
      throw new IllegalStateException("El evento no pertenece a la Marca del contexto");
    }
    jdbc.update(
        "INSERT INTO user_event (id, brand_id, consumer_id, type, occurred_at, attributes,"
            + " source_event_id) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?) ON CONFLICT (source_event_id)"
            + " DO NOTHING",
        event.id(),
        event.brandId().value(),
        event.consumerId().value(),
        event.type().name(),
        Timestamp.from(event.occurredAt()),
        json(event.attributes()),
        event.sourceEventId());
  }

  @Override
  public PageResult<UserEvent> search(UserEventCriteria criteria, PageQuery page) {
    StringBuilder where = new StringBuilder(" WHERE brand_id = ?");
    List<Object> params = new ArrayList<>();
    params.add(TenantContext.require().value());
    if (criteria.consumerId() != null) {
      where.append(" AND consumer_id = ?");
      params.add(criteria.consumerId().value());
    }
    if (criteria.type() != null) {
      where.append(" AND type = ?");
      params.add(criteria.type().name());
    }
    if (criteria.from() != null) {
      where.append(" AND occurred_at >= ?");
      params.add(Timestamp.from(criteria.from()));
    }
    if (criteria.to() != null) {
      where.append(" AND occurred_at < ?");
      params.add(Timestamp.from(criteria.to()));
    }
    Long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM user_event" + where, Long.class, params.toArray());
    List<Object> pageParams = new ArrayList<>(params);
    pageParams.add(page.size());
    pageParams.add((long) page.page() * page.size());
    List<UserEvent> content =
        jdbc.query(
            "SELECT id, brand_id, consumer_id, type, occurred_at, attributes::text AS attributes,"
                + " source_event_id FROM user_event"
                + where
                + " ORDER BY occurred_at DESC, id LIMIT ? OFFSET ?",
            (rs, i) -> map(rs),
            pageParams.toArray());
    return new PageResult<>(content, page.page(), page.size(), total == null ? 0 : total);
  }

  @Override
  public boolean existsSince(ConsumerId consumerId, UserEventType type, Instant since) {
    Boolean exists =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM user_event WHERE brand_id = ? AND consumer_id = ? AND"
                + " type = ? AND occurred_at >= ?)",
            Boolean.class,
            TenantContext.require().value(),
            consumerId.value(),
            type.name(),
            Timestamp.from(since));
    return Boolean.TRUE.equals(exists);
  }

  private UserEvent map(ResultSet rs) throws SQLException {
    try {
      return new UserEvent(
          rs.getObject("id", UUID.class),
          new BrandId(rs.getObject("brand_id", UUID.class)),
          new ConsumerId(rs.getObject("consumer_id", UUID.class)),
          UserEventType.valueOf(rs.getString("type")),
          rs.getTimestamp("occurred_at").toInstant(),
          mapper.readValue(rs.getString("attributes"), ATTRIBUTES),
          rs.getObject("source_event_id", UUID.class));
    } catch (JsonProcessingException e) {
      throw new SQLException("Atributos de evento no válidos", e);
    }
  }

  private String json(Map<String, String> attributes) {
    try {
      return mapper.writeValueAsString(attributes);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
