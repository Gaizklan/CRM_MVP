package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConsentLogRepository extends JpaRepository<ConsentLogJpaEntity, UUID> {

  List<ConsentLogJpaEntity> findByConsumerIdOrderByOccurredAtDescIdDesc(UUID consumerId);
}
