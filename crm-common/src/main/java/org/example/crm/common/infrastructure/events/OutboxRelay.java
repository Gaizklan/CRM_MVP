package org.example.crm.common.infrastructure.events;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.DomainEventHandler;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Entrega los eventos pendientes del outbox a los {@link DomainEventHandler} registrados.
 *
 * <ul>
 *   <li>Cada evento se procesa en su propia transacción, bloqueado con {@code FOR UPDATE SKIP
 *       LOCKED}: varias instancias pueden ejecutar el relay sin entregar dos veces el mismo evento.
 *   <li>Si un handler falla, la transacción se deshace, se incrementa {@code attempts} y se
 *       reintenta en la siguiente pasada, hasta {@code maxAttempts}.
 *   <li>Los handlers se ejecutan en la Marca del evento ({@link TenantContext}).
 * </ul>
 */
public class OutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final OutboxEventSerializer serializer;
  private final List<DomainEventHandler<?>> handlers;
  private final int batchSize;
  private final int maxAttempts;

  public OutboxRelay(
      JdbcTemplate jdbc,
      TransactionTemplate tx,
      OutboxEventSerializer serializer,
      List<DomainEventHandler<?>> handlers,
      int batchSize,
      int maxAttempts) {
    this.jdbc = jdbc;
    this.tx = tx;
    this.serializer = serializer;
    this.handlers = List.copyOf(handlers);
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
  }

  /** Procesa eventos pendientes hasta vaciar el outbox. Devuelve cuántos se han entregado. */
  public int relayPending() {
    int delivered = 0;
    List<Map<String, Object>> batch;
    do {
      batch =
          jdbc.queryForList(
              "SELECT id, brand_id FROM domain_event_outbox WHERE published_at IS NULL AND"
                  + " attempts < ? ORDER BY seq LIMIT ?",
              maxAttempts,
              batchSize);
      int deliveredInBatch = 0;
      for (Map<String, Object> row : batch) {
        UUID id = (UUID) row.get("id");
        BrandId brand = new BrandId((UUID) row.get("brand_id"));
        // La Marca se fija ANTES de abrir la transacción: Hibernate la asocia a la sesión.
        if (TenantContext.callAs(brand, () -> relayOne(id))) {
          deliveredInBatch++;
        }
      }
      delivered += deliveredInBatch;
      if (deliveredInBatch == 0) {
        break;
      }
    } while (batch.size() == batchSize);
    return delivered;
  }

  private boolean relayOne(UUID id) {
    try {
      Boolean done =
          tx.execute(
              status -> {
                List<Map<String, Object>> rows =
                    jdbc.queryForList(
                        "SELECT event_type, payload::text AS payload FROM"
                            + " domain_event_outbox WHERE id = ? AND published_at IS NULL FOR"
                            + " UPDATE SKIP LOCKED",
                        id);
                if (rows.isEmpty()) {
                  return false;
                }
                Map<String, Object> row = rows.get(0);
                DomainEvent event =
                    serializer.deserialize(
                        (String) row.get("event_type"), (String) row.get("payload"));
                dispatch(event, id);
                jdbc.update(
                    "UPDATE domain_event_outbox SET published_at = now(), attempts = attempts + 1,"
                        + " last_error = NULL WHERE id = ?",
                    id);
                return true;
              });
      return Boolean.TRUE.equals(done);
    } catch (RuntimeException e) {
      log.warn("Fallo entregando el evento {} del outbox: {}", id, e.toString());
      String error = String.valueOf(e);
      jdbc.update(
          "UPDATE domain_event_outbox SET attempts = attempts + 1, last_error = ? WHERE id = ?",
          error.length() > 2000 ? error.substring(0, 2000) : error,
          id);
      return false;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void dispatch(DomainEvent event, UUID id) {
    for (DomainEventHandler handler : handlers) {
      if (handler.eventType().isInstance(event)) {
        handler.handle(event, id);
      }
    }
  }
}
