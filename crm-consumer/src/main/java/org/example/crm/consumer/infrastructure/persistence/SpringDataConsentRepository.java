package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataConsentRepository
    extends JpaRepository<ConsentJpaEntity, ConsentJpaEntity.Key> {

  @Query("select c from ConsentJpaEntity c where c.key.consumerId = :consumerId")
  List<ConsentJpaEntity> findByConsumer(UUID consumerId);
}
