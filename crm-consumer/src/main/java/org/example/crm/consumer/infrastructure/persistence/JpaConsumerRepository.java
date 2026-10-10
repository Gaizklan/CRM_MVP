package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.common.application.crypto.PiiCipher;
import org.example.crm.consumer.application.ConsumerRepository;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.PreferenceChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA de consumidores. El filtro de Marca lo aplica Hibernate (ver ADR 0001). */
@Repository
@Transactional(readOnly = true)
class JpaConsumerRepository implements ConsumerRepository {

  private final SpringDataConsumerRepository consumers;
  private final SpringDataPreferenceHistoryRepository history;
  private final PiiCipher cipher;

  JpaConsumerRepository(
      SpringDataConsumerRepository consumers,
      SpringDataPreferenceHistoryRepository history,
      PiiCipher cipher) {
    this.consumers = consumers;
    this.history = history;
    this.cipher = cipher;
  }

  @Override
  public Optional<Consumer> findById(ConsumerId id) {
    return consumers.findById(id.value()).map(e -> e.toDomain(cipher));
  }

  @Override
  public Optional<Consumer> findByEmail(Email email) {
    return consumers.findByEmailHash(cipher.blindIndex(email.value())).map(e -> e.toDomain(cipher));
  }

  @Override
  public PageResult<Consumer> search(ConsumerSearchCriteria criteria, PageQuery page) {
    Page<ConsumerJpaEntity> result =
        consumers.findAll(
            specification(criteria),
            PageRequest.of(
                page.page(), page.size(), Sort.by("registeredAt").descending().and(Sort.by("id"))));
    return new PageResult<>(
        result.getContent().stream().map(e -> e.toDomain(cipher)).toList(),
        page.page(),
        page.size(),
        result.getTotalElements());
  }

  @Override
  @Transactional
  public Consumer save(Consumer consumer, List<PreferenceChange> preferenceChanges) {
    ConsumerJpaEntity entity =
        consumers
            .findById(consumer.id().value())
            .map(
                existing -> {
                  existing.apply(consumer, cipher);
                  return existing;
                })
            .orElseGet(() -> ConsumerJpaEntity.create(consumer, cipher));
    ConsumerJpaEntity saved = consumers.saveAndFlush(entity);
    history.saveAll(preferenceChanges.stream().map(PreferenceHistoryJpaEntity::from).toList());
    return saved.toDomain(cipher);
  }

  @Override
  public List<PreferenceChange> preferenceHistory(ConsumerId id) {
    return history.findByConsumerIdOrderByChangedAtDesc(id.value()).stream()
        .map(PreferenceHistoryJpaEntity::toDomain)
        .toList();
  }

  private Specification<ConsumerJpaEntity> specification(ConsumerSearchCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (c.email() != null) {
        predicates.add(cb.equal(root.get("emailHash"), cipher.blindIndex(c.email().value())));
      }
      if (c.status() != null) {
        predicates.add(cb.equal(root.get("status"), c.status()));
      }
      if (c.language() != null) {
        predicates.add(cb.equal(root.get("language"), c.language().value()));
      }
      if (c.segment() != null) {
        predicates.add(cb.equal(root.get("segment"), c.segment()));
      }
      if (c.registeredFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("registeredAt"), c.registeredFrom()));
      }
      if (c.registeredTo() != null) {
        predicates.add(cb.lessThan(root.get("registeredAt"), c.registeredTo()));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
