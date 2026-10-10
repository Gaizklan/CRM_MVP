package org.example.crm.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.junit.jupiter.api.Test;

class InternalUserServiceTest {

  static final class InMemoryUsers implements InternalUserRepository {
    final List<InternalUser> users = new ArrayList<>();

    @Override
    public Optional<InternalUser> findById(InternalUserId id) {
      return users.stream().filter(u -> u.id().equals(id)).findFirst();
    }

    @Override
    public Optional<InternalUser> findByUsername(String username) {
      return users.stream().filter(u -> u.username().equals(username)).findFirst();
    }

    @Override
    public List<InternalUser> findAll() {
      return List.copyOf(users);
    }

    @Override
    public boolean existsAny() {
      return !users.isEmpty();
    }

    @Override
    public InternalUser save(InternalUser user) {
      users.removeIf(u -> u.id().equals(user.id()));
      users.add(user);
      return user;
    }
  }

  private final Brand brandA = Brand.create("marca-a", "Marca A", null, "es");
  private final BrandLookup brands =
      new BrandLookup() {
        @Override
        public Optional<Brand> findById(BrandId id) {
          return id.equals(brandA.id()) ? Optional.of(brandA) : Optional.empty();
        }

        @Override
        public Optional<Brand> findByCode(String code) {
          return Optional.empty();
        }

        @Override
        public Optional<Brand> findByDomain(String host) {
          return Optional.empty();
        }
      };
  private final InMemoryUsers users = new InMemoryUsers();
  private final InternalUserService service =
      new InternalUserService(users, raw -> "{test}" + raw.length(), brands);

  private InternalUserCommand local(String username) {
    return new InternalUserCommand(
        username,
        "una-clave-segura-1",
        SecurityLevel.LOCAL_USER,
        brandA.id(),
        Set.of(FunctionalRole.PRODUCER),
        true);
  }

  @Test
  void createsUserWithHashedPassword() {
    InternalUser user = service.create(local("local.a"));

    assertThat(user.passwordHash()).isEqualTo("{test}18").doesNotContain("una-clave");
    assertThat(service.list()).containsExactly(user);
    assertThat(service.get(user.id())).isEqualTo(user);
  }

  @Test
  void rejectsDuplicatesWeakPasswordsAndUnknownBrands() {
    service.create(local("local.a"));

    assertThatThrownBy(() -> service.create(local("local.a")))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    new InternalUserCommand(
                        "x.y.z", "corta", SecurityLevel.ADMIN, null, Set.of(), true)))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    new InternalUserCommand(
                        "x.y.z",
                        "una-clave-segura-1",
                        SecurityLevel.LOCAL_USER,
                        BrandId.random(),
                        Set.of(),
                        true)))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Marca desconocida");
  }

  @Test
  void updatesLevelRolesEnabledAndPassword() {
    InternalUser user = service.create(local("local.a"));

    InternalUser updated =
        service.update(
            user.id(),
            new InternalUserCommand(
                null,
                "otra-clave-segura-22",
                SecurityLevel.GLOBAL_USER,
                null,
                Set.of(FunctionalRole.DEVELOPER),
                false));

    assertThat(updated.level()).isEqualTo(SecurityLevel.GLOBAL_USER);
    assertThat(updated.roles()).containsExactly(FunctionalRole.DEVELOPER);
    assertThat(updated.enabled()).isFalse();
    assertThat(updated.passwordHash()).isEqualTo("{test}20");

    InternalUser reenabled =
        service.update(
            user.id(), new InternalUserCommand(null, null, SecurityLevel.ADMIN, null, null, true));
    assertThat(reenabled.enabled()).isTrue();
    assertThat(reenabled.passwordHash()).isEqualTo("{test}20");
  }

  @Test
  void unknownUserIsNotFound() {
    assertThatThrownBy(() -> service.get(InternalUserId.random()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void bootstrapsAdminOnlyWhenEmpty() {
    assertThat(service.bootstrapAdmin("admin", "una-clave-segura-1")).isTrue();
    assertThat(users.findByUsername("admin"))
        .get()
        .extracting(InternalUser::level)
        .isEqualTo(SecurityLevel.ADMIN);
    assertThat(service.bootstrapAdmin("admin2", "una-clave-segura-1")).isFalse();
  }
}
