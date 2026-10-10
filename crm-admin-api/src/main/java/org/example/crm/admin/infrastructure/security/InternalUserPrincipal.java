package org.example.crm.admin.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.example.crm.admin.domain.InternalUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario interno autenticado. Autoridades: {@code LEVEL_<nivel>} y {@code ROLE_<rol funcional>}.
 */
public final class InternalUserPrincipal implements UserDetails {

  public static final String LEVEL_PREFIX = "LEVEL_";
  public static final String ROLE_PREFIX = "ROLE_";

  private final InternalUser user;
  private final List<GrantedAuthority> authorities;

  public InternalUserPrincipal(InternalUser user) {
    this.user = user;
    List<GrantedAuthority> list = new ArrayList<>();
    list.add(new SimpleGrantedAuthority(LEVEL_PREFIX + user.level().name()));
    user.roles().stream()
        .sorted()
        .forEach(role -> list.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.name())));
    this.authorities = List.copyOf(list);
  }

  public InternalUser user() {
    return user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return user.passwordHash();
  }

  @Override
  public String getUsername() {
    return user.username();
  }

  @Override
  public boolean isEnabled() {
    return user.enabled();
  }
}
