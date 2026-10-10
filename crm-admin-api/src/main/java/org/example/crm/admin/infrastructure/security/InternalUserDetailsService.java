package org.example.crm.admin.infrastructure.security;

import org.example.crm.admin.application.InternalUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class InternalUserDetailsService implements UserDetailsService {

  private final InternalUserRepository users;

  InternalUserDetailsService(InternalUserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    return users
        .findByUsername(username)
        .map(InternalUserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("Credenciales no válidas"));
  }
}
