package org.example.crm.consumer.application;

import java.time.Clock;
import java.util.List;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.NotFoundException;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.PreferenceChange;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.consumer.domain.ProfileUpdate;

/**
 * Casos de uso de consumidores. Opera siempre en la Marca del {@link TenantContext}. Los cambios
 * publican {@code ConsumerProfileChanged} tras persistir.
 */
@org.springframework.transaction.annotation.Transactional
public class ConsumerService {

  private final ConsumerRepository consumers;
  private final DomainEventPublisher events;
  private final Clock clock;

  public ConsumerService(ConsumerRepository consumers, DomainEventPublisher events, Clock clock) {
    this.consumers = consumers;
    this.events = events;
    this.clock = clock;
  }

  public Consumer register(Email email, PhoneNumber phone, LanguageTag language) {
    ensureEmailFree(email, null);
    var consumer = Consumer.register(TenantContext.require(), email, phone, language, clock);
    return persist(consumer);
  }

  public Consumer get(ConsumerId id) {
    TenantContext.require();
    return consumers
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Consumidor no encontrado: " + id));
  }

  /** Consumidor de la Marca actual, si existe (vacío sin Marca en contexto). */
  public java.util.Optional<Consumer> find(ConsumerId id) {
    return TenantContext.current().isEmpty() ? java.util.Optional.empty() : consumers.findById(id);
  }

  public PageResult<Consumer> search(ConsumerSearchCriteria criteria, PageQuery page) {
    TenantContext.require();
    return consumers.search(criteria, page);
  }

  public Consumer updateProfile(ConsumerId id, ProfileUpdate update) {
    if (update.email() != null) {
      ensureEmailFree(update.email(), id);
    }
    return modify(id, c -> c.updateProfile(update, clock));
  }

  public Consumer setPreference(
      ConsumerId id, String key, PreferenceValue value, String changedBy) {
    return modify(id, c -> c.setPreference(key, value, changedBy, clock));
  }

  public Consumer removePreference(ConsumerId id, String key, String changedBy) {
    return modify(id, c -> c.removePreference(key, changedBy, clock));
  }

  public List<PreferenceChange> preferenceHistory(ConsumerId id) {
    get(id);
    return consumers.preferenceHistory(id);
  }

  private Consumer modify(ConsumerId id, java.util.function.Consumer<Consumer> change) {
    var consumer = get(id);
    change.accept(consumer);
    return persist(consumer);
  }

  private Consumer persist(Consumer consumer) {
    List<PreferenceChange> history = consumer.pullPreferenceChanges();
    var pending = consumer.pullEvents();
    var saved = consumers.save(consumer, history);
    events.publishAll(pending);
    return saved;
  }

  private void ensureEmailFree(Email email, ConsumerId self) {
    consumers
        .findByEmail(email)
        .filter(other -> !other.id().equals(self))
        .ifPresent(
            other -> {
              throw new ConflictException("Ya existe un consumidor con ese email en la Marca");
            });
  }
}
