package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

  Optional<SubscriptionJpaEntity> findByConsumerIdAndServiceIdAndUnsubscribedAtIsNull(
      UUID consumerId, UUID serviceId);

  List<SubscriptionJpaEntity> findByConsumerIdOrderBySubscribedAtDesc(UUID consumerId);

  @Query(
      """
      select s.code from SubscriptionJpaEntity sub, SubscriptionServiceJpaEntity s
      where sub.serviceId = s.id and sub.consumerId = :consumerId and sub.unsubscribedAt is null
      """)
  List<String> activeServiceCodes(UUID consumerId);
}
