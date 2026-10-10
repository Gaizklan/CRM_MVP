package org.example.crm.consumer.infrastructure.persistence;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.consumer.application.ConsentRepository;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentStatus;
import org.example.crm.consumer.domain.Consents;
import org.example.crm.consumer.domain.ConsumerId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaConsentRepository implements ConsentRepository {

  private final SpringDataConsentRepository consents;
  private final SpringDataConsentLogRepository log;

  JpaConsentRepository(SpringDataConsentRepository consents, SpringDataConsentLogRepository log) {
    this.consents = consents;
    this.log = log;
  }

  @Override
  public Consents load(ConsumerId consumerId) {
    Map<ConsentChannel, ConsentStatus> current = new EnumMap<>(ConsentChannel.class);
    consents
        .findByConsumer(consumerId.value())
        .forEach(e -> current.put(e.channel(), e.toStatus()));
    return Consents.restore(TenantContext.require(), consumerId, current);
  }

  @Override
  @Transactional
  public void save(Consents state, List<ConsentChange> changes) {
    changes.stream()
        .map(ConsentChange::channel)
        .distinct()
        .forEach(
            channel -> {
              ConsentJpaEntity entity =
                  consents
                      .findById(new ConsentJpaEntity.Key(state.consumerId().value(), channel))
                      .orElseGet(() -> new ConsentJpaEntity(state.consumerId().value(), channel));
              entity.apply(state.status(channel).orElseThrow());
              consents.save(entity);
            });
    log.saveAll(changes.stream().map(ConsentLogJpaEntity::from).toList());
  }

  @Override
  public List<ConsentChange> history(ConsumerId consumerId) {
    return log.findByConsumerIdOrderByOccurredAtDescIdDesc(consumerId.value()).stream()
        .map(ConsentLogJpaEntity::toDomain)
        .toList();
  }
}
