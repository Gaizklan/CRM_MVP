package org.example.crm.common.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBrandRepository extends JpaRepository<BrandJpaEntity, UUID> {

  Optional<BrandJpaEntity> findByCode(String code);

  Optional<BrandJpaEntity> findByDomain(String domain);
}
