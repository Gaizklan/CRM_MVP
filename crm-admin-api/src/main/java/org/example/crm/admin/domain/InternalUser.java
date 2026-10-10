package org.example.crm.admin.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;

/**
 * Usuario interno de la plataforma (gestor CRM, productor, revisor, desarrollador, administrador).
 *
 * <p>Invariantes: un {@link SecurityLevel#LOCAL_USER} pertenece exactamente a una Marca; {@link
 * SecurityLevel#ADMIN} y {@link SecurityLevel#GLOBAL_USER} no tienen Marca. La contraseña se guarda
 * solo como hash.
 */
public final class InternalUser {

  private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._-]{2,63}");

  private final InternalUserId id;
  private final String username;
  private String passwordHash;
  private SecurityLevel level;
  private BrandId brand;
  private Set<FunctionalRole> roles;
  private boolean enabled;

  private InternalUser(
      InternalUserId id,
      String username,
      String passwordHash,
      SecurityLevel level,
      BrandId brand,
      Set<FunctionalRole> roles,
      boolean enabled) {
    this.id = Objects.requireNonNull(id, "id");
    this.username = validateUsername(username);
    this.passwordHash = requireHash(passwordHash);
    assignLevel(level, brand);
    this.roles = copy(roles);
    this.enabled = enabled;
  }

  public static InternalUser create(
      String username,
      String passwordHash,
      SecurityLevel level,
      BrandId brand,
      Set<FunctionalRole> roles) {
    return new InternalUser(
        InternalUserId.random(), username, passwordHash, level, brand, roles, true);
  }

  public static InternalUser restore(
      InternalUserId id,
      String username,
      String passwordHash,
      SecurityLevel level,
      BrandId brand,
      Set<FunctionalRole> roles,
      boolean enabled) {
    return new InternalUser(id, username, passwordHash, level, brand, roles, enabled);
  }

  /** Indica si el usuario puede operar sobre los datos de la Marca indicada. */
  public boolean canAccessBrand(BrandId target) {
    if (!enabled || target == null) {
      return false;
    }
    return level != SecurityLevel.LOCAL_USER || brand.equals(target);
  }

  /** Solo ADMIN gestiona usuarios internos y Marcas. */
  public boolean canManageUsersAndBrands() {
    return enabled && level == SecurityLevel.ADMIN;
  }

  public boolean hasRole(FunctionalRole role) {
    return roles.contains(role);
  }

  public void changeLevel(SecurityLevel newLevel, BrandId newBrand) {
    assignLevel(newLevel, newBrand);
  }

  public void changeRoles(Set<FunctionalRole> newRoles) {
    this.roles = copy(newRoles);
  }

  public void changePasswordHash(String newHash) {
    this.passwordHash = requireHash(newHash);
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  public InternalUserId id() {
    return id;
  }

  public String username() {
    return username;
  }

  public String passwordHash() {
    return passwordHash;
  }

  public SecurityLevel level() {
    return level;
  }

  /** Marca del usuario; solo presente para {@link SecurityLevel#LOCAL_USER}. */
  public Optional<BrandId> brand() {
    return Optional.ofNullable(brand);
  }

  public Set<FunctionalRole> roles() {
    return Set.copyOf(roles);
  }

  public boolean enabled() {
    return enabled;
  }

  private void assignLevel(SecurityLevel newLevel, BrandId newBrand) {
    Objects.requireNonNull(newLevel, "level");
    if (newLevel == SecurityLevel.LOCAL_USER && newBrand == null) {
      throw new DomainException("Un usuario LOCAL_USER debe pertenecer a una Marca");
    }
    if (newLevel != SecurityLevel.LOCAL_USER && newBrand != null) {
      throw new DomainException("Solo los usuarios LOCAL_USER se asocian a una Marca");
    }
    this.level = newLevel;
    this.brand = newBrand;
  }

  private static String validateUsername(String username) {
    if (username == null || !USERNAME.matcher(username).matches()) {
      throw new DomainException(
          "Nombre de usuario no válido (3-64 caracteres [a-z0-9._-], en minúsculas)");
    }
    return username;
  }

  private static String requireHash(String hash) {
    if (hash == null || hash.isBlank()) {
      throw new DomainException("Se requiere el hash de la contraseña");
    }
    return hash;
  }

  private static Set<FunctionalRole> copy(Set<FunctionalRole> roles) {
    return roles == null || roles.isEmpty()
        ? EnumSet.noneOf(FunctionalRole.class)
        : EnumSet.copyOf(roles);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof InternalUser other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
