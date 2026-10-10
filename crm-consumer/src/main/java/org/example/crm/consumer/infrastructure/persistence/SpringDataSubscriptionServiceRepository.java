package org.example.crm.consumer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSubscriptionServiceRepository
    extends JpaRepository<SubscriptionServiceJpaEntity, UUID> {

  Optional<SubscriptionServiceJpaEntity> findByCode(String code);
}
