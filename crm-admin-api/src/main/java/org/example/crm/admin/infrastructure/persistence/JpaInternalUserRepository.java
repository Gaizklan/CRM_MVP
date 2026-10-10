package org.example.crm.admin.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class JpaInternalUserRepository implements InternalUserRepository {

  private final SpringDataInternalUserRepository repository;

  JpaInternalUserRepository(SpringDataInternalUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<InternalUser> findById(InternalUserId id) {
    return repository.findById(id.value()).map(InternalUserJpaEntity::toDomain);
  }

  @Override
  public Optional<InternalUser> findByUsername(String username) {
    return repository.findByUsername(username).map(InternalUserJpaEntity::toDomain);
  }

  @Override
  public List<InternalUser> findAll() {
    return repository.findAll(Sort.by("username")).stream()
        .map(InternalUserJpaEntity::toDomain)
        .toList();
  }

  @Override
  public boolean existsAny() {
    return repository.count() > 0;
  }

  @Override
  @Transactional
  public InternalUser save(InternalUser user) {
    return repository.save(InternalUserJpaEntity.from(user)).toDomain();
  }
}
