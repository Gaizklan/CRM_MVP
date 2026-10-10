package org.example.crm.tenancy;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TenantProbeRepository extends JpaRepository<TenantProbeEntity, UUID> {

  List<TenantProbeEntity> findByLabel(String label);

  @Query("select p from TenantProbeEntity p order by p.label")
  List<TenantProbeEntity> findAllOrdered();
}
