package org.example.crm.consumer.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentStatus;
import org.example.crm.consumer.domain.Consents;
import org.example.crm.consumer.domain.ConsumerId;

/** Repositorio de consentimientos en memoria (para tests). */
public class InMemoryConsentRepository implements ConsentRepository {

  final Map<ConsumerId, Map<ConsentChannel, ConsentStatus>> state = new HashMap<>();
  final List<ConsentChange> log = new ArrayList<>();

  @Override
  public Consents load(ConsumerId consumerId) {
    return Consents.restore(
        TenantContext.require(), consumerId, state.getOrDefault(consumerId, Map.of()));
  }

  @Override
  public void save(Consents consents, List<ConsentChange> changes) {
    state.put(consents.consumerId(), new HashMap<>(consents.current()));
    log.addAll(changes);
  }

  @Override
  public List<ConsentChange> history(ConsumerId consumerId) {
    return log.stream().filter(c -> c.consumerId().equals(consumerId)).toList().reversed();
  }
}
