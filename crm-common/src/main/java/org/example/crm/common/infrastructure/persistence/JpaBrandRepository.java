package org.example.crm.common.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.example.crm.common.application.tenant.BrandRepository;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del catálogo de Marcas. */
@Repository
@Transactional(readOnly = true)
public class JpaBrandRepository implements BrandRepository {

  private final SpringDataBrandRepository repository;

  JpaBrandRepository(SpringDataBrandRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Brand> findById(BrandId id) {
    return repository.findById(id.value()).map(BrandJpaEntity::toDomain);
  }

  @Override
  public Optional<Brand> findByCode(String code) {
    return repository.findByCode(code).map(BrandJpaEntity::toDomain);
  }

  @Override
  public Optional<Brand> findByDomain(String host) {
    return repository.findByDomain(host).map(BrandJpaEntity::toDomain);
  }

  @Override
  public List<Brand> findAll() {
    return repository.findAll().stream().map(BrandJpaEntity::toDomain).toList();
  }

  @Override
  @Transactional
  public Brand save(Brand brand) {
    return repository.save(BrandJpaEntity.from(brand)).toDomain();
  }
}
