package org.example.crm.consumer.application;

import java.util.List;
import java.util.Optional;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.PreferenceChange;

/** Puerto de salida: persistencia de consumidores (siempre en la Marca del contexto). */
public interface ConsumerRepository {

  Optional<Consumer> findById(ConsumerId id);

  Optional<Consumer> findByEmail(Email email);

  PageResult<Consumer> search(ConsumerSearchCriteria criteria, PageQuery page);

  /** Guarda el consumidor y las entradas de historial de preferencias indicadas. */
  Consumer save(Consumer consumer, List<PreferenceChange> preferenceChanges);

  /** Historial de cambios de preferencias, más reciente primero. */
  List<PreferenceChange> preferenceHistory(ConsumerId id);
}
