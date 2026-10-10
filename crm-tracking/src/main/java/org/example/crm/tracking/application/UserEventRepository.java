package org.example.crm.tracking.application;

import java.time.Instant;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.tracking.domain.UserEvent;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;

/** Puerto de salida: almacenamiento de eventos de usuario (Marca del contexto). */
public interface UserEventRepository {

  /** Guarda el evento; si ya existe uno con el mismo {@code sourceEventId}, no hace nada. */
  void save(UserEvent event);

  PageResult<UserEvent> search(UserEventCriteria criteria, PageQuery page);

  boolean existsSince(ConsumerId consumerId, UserEventType type, Instant since);
}
