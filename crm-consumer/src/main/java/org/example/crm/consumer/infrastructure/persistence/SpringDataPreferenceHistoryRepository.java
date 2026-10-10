package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPreferenceHistoryRepository
    extends JpaRepository<PreferenceHistoryJpaEntity, UUID> {

  List<PreferenceHistoryJpaEntity> findByConsumerIdOrderByChangedAtDesc(UUID consumerId);
}
