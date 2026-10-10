package org.example.crm.consumer.application;

import java.util.List;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.Consents;
import org.example.crm.consumer.domain.ConsumerId;

/** Puerto de salida: estado vigente y registro de auditoría de consentimientos. */
public interface ConsentRepository {

  /** Consentimientos vigentes (vacío si no hay ninguno registrado). */
  Consents load(ConsumerId consumerId);

  /** Persiste el estado vigente y añade los cambios al registro de auditoría. */
  void save(Consents consents, List<ConsentChange> changes);

  /** Registro de auditoría del consumidor, más reciente primero. */
  List<ConsentChange> history(ConsumerId consumerId);
}
