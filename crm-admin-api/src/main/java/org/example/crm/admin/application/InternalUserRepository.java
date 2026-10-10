package org.example.crm.admin.application;

import java.util.List;
import java.util.Optional;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;

/** Puerto de salida: persistencia de usuarios internos. */
public interface InternalUserRepository {

  Optional<InternalUser> findById(InternalUserId id);

  Optional<InternalUser> findByUsername(String username);

  List<InternalUser> findAll();

  boolean existsAny();

  InternalUser save(InternalUser user);
}
