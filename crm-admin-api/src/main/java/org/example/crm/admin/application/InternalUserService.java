package org.example.crm.admin.application;

import java.util.List;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;
import org.example.crm.admin.domain.PasswordPolicy;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;

/** Casos de uso de gestión de usuarios internos (reservados a ADMIN en la capa web). */
public class InternalUserService {

  private final InternalUserRepository users;
  private final PasswordHasher hasher;
  private final BrandLookup brands;

  public InternalUserService(
      InternalUserRepository users, PasswordHasher hasher, BrandLookup brands) {
    this.users = users;
    this.hasher = hasher;
    this.brands = brands;
  }

  public List<InternalUser> list() {
    return users.findAll();
  }

  public InternalUser get(InternalUserId id) {
    return users
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Usuario interno no encontrado: " + id));
  }

  public InternalUser create(InternalUserCommand command) {
    PasswordPolicy.validate(command.password());
    requireExistingBrand(command.brand());
    if (users.findByUsername(command.username()).isPresent()) {
      throw new ConflictException("Ya existe el usuario " + command.username());
    }
    InternalUser user =
        InternalUser.create(
            command.username(),
            hasher.hash(command.password()),
            command.level(),
            command.brand(),
            command.roles());
    return users.save(user);
  }

  public InternalUser update(InternalUserId id, InternalUserCommand command) {
    InternalUser user = get(id);
    requireExistingBrand(command.brand());
    user.changeLevel(command.level(), command.brand());
    user.changeRoles(command.roles());
    if (command.password() != null) {
      PasswordPolicy.validate(command.password());
      user.changePasswordHash(hasher.hash(command.password()));
    }
    if (Boolean.FALSE.equals(command.enabled())) {
      user.disable();
    } else if (Boolean.TRUE.equals(command.enabled())) {
      user.enable();
    }
    return users.save(user);
  }

  /**
   * Crea el primer ADMIN si no existe ningún usuario interno. Se usa en el arranque con
   * credenciales aportadas por configuración externa.
   */
  public boolean bootstrapAdmin(String username, String password) {
    if (users.existsAny()) {
      return false;
    }
    create(
        new InternalUserCommand(
            username,
            password,
            org.example.crm.admin.domain.SecurityLevel.ADMIN,
            null,
            java.util.Set.of(),
            true));
    return true;
  }

  private void requireExistingBrand(BrandId brand) {
    if (brand != null && brands.findById(brand).isEmpty()) {
      throw new DomainException("Marca desconocida: " + brand);
    }
  }
}
