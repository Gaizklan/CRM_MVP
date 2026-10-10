package org.example.crm.admin.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInternalUserRepository extends JpaRepository<InternalUserJpaEntity, UUID> {

  Optional<InternalUserJpaEntity> findByUsername(String username);
}
