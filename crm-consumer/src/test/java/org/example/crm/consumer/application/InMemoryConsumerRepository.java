package org.example.crm.consumer.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.PreferenceChange;

/** Repositorio en memoria que respeta la Marca del contexto (para tests). */
public class InMemoryConsumerRepository implements ConsumerRepository {

  private final Map<ConsumerId, Consumer> consumers = new LinkedHashMap<>();
  private final List<PreferenceChange> history = new ArrayList<>();

  private boolean visible(Consumer c) {
    return TenantContext.current().map(b -> b.equals(c.brandId())).orElse(false);
  }

  @Override
  public Optional<Consumer> findById(ConsumerId id) {
    return Optional.ofNullable(consumers.get(id)).filter(this::visible);
  }

  @Override
  public Optional<Consumer> findByEmail(Email email) {
    return consumers.values().stream()
        .filter(this::visible)
        .filter(c -> c.email().equals(email))
        .findFirst();
  }

  @Override
  public PageResult<Consumer> search(ConsumerSearchCriteria c, PageQuery page) {
    List<Consumer> all =
        consumers.values().stream()
            .filter(this::visible)
            .filter(x -> c.email() == null || x.email().equals(c.email()))
            .filter(x -> c.status() == null || x.status() == c.status())
            .filter(x -> c.segment() == null || x.segment().equals(Optional.of(c.segment())))
            .toList();
    List<Consumer> content =
        all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList();
    return new PageResult<>(content, page.page(), page.size(), all.size());
  }

  @Override
  public Consumer save(Consumer consumer, List<PreferenceChange> preferenceChanges) {
    consumers.put(consumer.id(), consumer);
    history.addAll(preferenceChanges);
    return consumer;
  }

  @Override
  public List<PreferenceChange> preferenceHistory(ConsumerId id) {
    return history.stream()
        .filter(h -> h.consumerId().equals(id))
        .sorted(Comparator.comparing(PreferenceChange::changedAt).reversed())
        .toList();
  }
}
