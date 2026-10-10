package org.example.crm.admin.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.domain.BrandId;

/**
 * Tabla {@code internal_user}. Es una tabla de plataforma (no filtrada por Marca): los usuarios
 * GLOBAL_USER y ADMIN no pertenecen a ninguna Marca.
 */
@Entity
@Table(name = "internal_user")
public class InternalUserJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 64)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private SecurityLevel level;

  @Column(name = "brand_id")
  private UUID brandId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "internal_user_role", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 32)
  private Set<FunctionalRole> roles = EnumSet.noneOf(FunctionalRole.class);

  @Column(nullable = false)
  private boolean enabled;

  protected InternalUserJpaEntity() {}

  static InternalUserJpaEntity from(InternalUser user) {
    InternalUserJpaEntity e = new InternalUserJpaEntity();
    e.id = user.id().value();
    e.username = user.username();
    e.passwordHash = user.passwordHash();
    e.level = user.level();
    e.brandId = user.brand().map(BrandId::value).orElse(null);
    e.roles =
        user.roles().isEmpty()
            ? EnumSet.noneOf(FunctionalRole.class)
            : EnumSet.copyOf(user.roles());
    e.enabled = user.enabled();
    return e;
  }

  InternalUser toDomain() {
    return InternalUser.restore(
        new InternalUserId(id),
        username,
        passwordHash,
        level,
        brandId == null ? null : new BrandId(brandId),
        roles,
        enabled);
  }
}
