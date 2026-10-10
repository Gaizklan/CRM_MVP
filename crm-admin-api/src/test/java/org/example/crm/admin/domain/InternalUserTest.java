package org.example.crm.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InternalUserTest {

  private final BrandId brandA = BrandId.random();
  private final BrandId brandB = BrandId.random();

  @Test
  void localUserOnlyAccessesItsBrand() {
    InternalUser user =
        InternalUser.create(
            "local.a",
            "{bcrypt}x",
            SecurityLevel.LOCAL_USER,
            brandA,
            Set.of(FunctionalRole.PRODUCER));

    assertThat(user.canAccessBrand(brandA)).isTrue();
    assertThat(user.canAccessBrand(brandB)).isFalse();
    assertThat(user.canAccessBrand(null)).isFalse();
    assertThat(user.canManageUsersAndBrands()).isFalse();
    assertThat(user.brand()).contains(brandA);
    assertThat(user.hasRole(FunctionalRole.PRODUCER)).isTrue();
    assertThat(user.hasRole(FunctionalRole.DEVELOPER)).isFalse();
  }

  @Test
  void globalUserAccessesAllBrandsButCannotManage() {
    InternalUser user =
        InternalUser.create("global", "{bcrypt}x", SecurityLevel.GLOBAL_USER, null, Set.of());

    assertThat(user.canAccessBrand(brandA)).isTrue();
    assertThat(user.canAccessBrand(brandB)).isTrue();
    assertThat(user.canManageUsersAndBrands()).isFalse();
    assertThat(user.brand()).isEmpty();
    assertThat(user.roles()).isEmpty();
  }

  @Test
  void adminAccessesAllAndManages() {
    InternalUser user = InternalUser.create("admin", "{bcrypt}x", SecurityLevel.ADMIN, null, null);

    assertThat(user.canAccessBrand(brandB)).isTrue();
    assertThat(user.canManageUsersAndBrands()).isTrue();
  }

  @Test
  void disabledUserHasNoAccess() {
    InternalUser user = InternalUser.create("admin", "{bcrypt}x", SecurityLevel.ADMIN, null, null);
    user.disable();

    assertThat(user.enabled()).isFalse();
    assertThat(user.canAccessBrand(brandA)).isFalse();
    assertThat(user.canManageUsersAndBrands()).isFalse();
    user.enable();
    assertThat(user.canManageUsersAndBrands()).isTrue();
  }

  @Test
  void levelAndBrandInvariants() {
    assertThatThrownBy(
            () -> InternalUser.create("local", "{bcrypt}x", SecurityLevel.LOCAL_USER, null, null))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(
            () ->
                InternalUser.create("global", "{bcrypt}x", SecurityLevel.GLOBAL_USER, brandA, null))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void changesLevelRolesAndPassword() {
    InternalUser user =
        InternalUser.create("user1", "{bcrypt}x", SecurityLevel.GLOBAL_USER, null, Set.of());

    user.changeLevel(SecurityLevel.LOCAL_USER, brandB);
    user.changeRoles(Set.of(FunctionalRole.CRM_MANAGER, FunctionalRole.CONTENT_REVIEWER));
    user.changePasswordHash("{bcrypt}y");

    assertThat(user.level()).isEqualTo(SecurityLevel.LOCAL_USER);
    assertThat(user.brand()).contains(brandB);
    assertThat(user.roles())
        .containsExactlyInAnyOrder(FunctionalRole.CRM_MANAGER, FunctionalRole.CONTENT_REVIEWER);
    assertThat(user.passwordHash()).isEqualTo("{bcrypt}y");
    assertThatThrownBy(() -> user.changePasswordHash(" ")).isInstanceOf(DomainException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ab", "Admin", "with space", "-dash"})
  void rejectsInvalidUsernames(String username) {
    assertThatThrownBy(
            () -> InternalUser.create(username, "{bcrypt}x", SecurityLevel.ADMIN, null, null))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void restoreAndEquality() {
    InternalUserId id = InternalUserId.random();
    InternalUser a =
        InternalUser.restore(id, "user1", "h", SecurityLevel.ADMIN, null, Set.of(), false);
    InternalUser b =
        InternalUser.restore(id, "user1", "h2", SecurityLevel.ADMIN, null, Set.of(), true);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a.enabled()).isFalse();
    assertThat(a.id()).isEqualTo(id);
    assertThat(id.toString()).isEqualTo(id.value().toString());
    assertThat(a.username()).isEqualTo("user1");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"short", "            "})
  void passwordPolicyRejectsWeakPasswords(String password) {
    assertThatThrownBy(() -> PasswordPolicy.validate(password)).isInstanceOf(DomainException.class);
  }

  @Test
  void passwordPolicyAcceptsLongPasswordsAndRejectsHuge() {
    PasswordPolicy.validate("una-clave-larga-1");
    assertThatThrownBy(() -> PasswordPolicy.validate("x".repeat(129)))
        .isInstanceOf(DomainException.class);
  }
}
