package org.example.crm.admin.infrastructure.security;

import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.application.PasswordHasher;
import org.example.crm.common.application.tenant.BrandLookup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Seguridad de usuarios internos.
 *
 * <ul>
 *   <li>{@code /api/**}: API REST de administración sin estado (HTTP Basic), sin CSRF porque no usa
 *       cookies de sesión. {@code /api/admin/**} solo para nivel ADMIN.
 *   <li>Resto: login de administración por formulario ({@code /login}) con sesión y CSRF.
 * </ul>
 *
 * Autoridades: {@code LEVEL_<nivel>} y {@code ROLE_<rol funcional>}; la Marca se restringe en
 * {@link BrandAccessFilter}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AdminSecurityConfiguration {

  public static final String ADMIN = "LEVEL_ADMIN";

  @Bean
  @Order(1)
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/admin/**")
                    .hasAuthority(ADMIN)
                    .anyRequest()
                    .authenticated())
        .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorized()))
        .addFilterAfter(new BrandAccessFilter(), AuthorizationFilter.class);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .formLogin(form -> form.permitAll())
        .logout(logout -> logout.permitAll());
    return http.build();
  }

  private static HttpStatusEntryPoint unauthorized() {
    return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    // BCrypt por defecto, con prefijo {id} para poder migrar a Argon2 sin invalidar hashes.
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  PasswordHasher passwordHasher(PasswordEncoder encoder) {
    return encoder::encode;
  }

  @Bean
  InternalUserService internalUserService(
      InternalUserRepository users, PasswordHasher hasher, BrandLookup brands) {
    return new InternalUserService(users, hasher, brands);
  }
}
