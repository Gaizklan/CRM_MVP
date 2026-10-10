package org.example.crm.consumer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataConsumerRepository
    extends JpaRepository<ConsumerJpaEntity, UUID>, JpaSpecificationExecutor<ConsumerJpaEntity> {

  Optional<ConsumerJpaEntity> findByEmailHash(String emailHash);
}
