package org.example.crm.admin.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restringe la Marca sobre la que opera un usuario interno autenticado.
 *
 * <ul>
 *   <li>LOCAL_USER: si no selecciona Marca se usa la suya; seleccionar otra Marca → 403.
 *   <li>GLOBAL_USER y ADMIN: pueden seleccionar cualquier Marca.
 * </ul>
 */
public class BrandAccessFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<InternalUser> user = currentUser();
    if (user.isEmpty()) {
      chain.doFilter(request, response);
      return;
    }
    Optional<BrandId> selected = TenantContext.current();
    if (selected.isPresent() && !user.get().canAccessBrand(selected.get())) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response
          .getWriter()
          .write(
              "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"El usuario no tiene acceso a"
                  + " la Marca seleccionada\"}");
      return;
    }
    Optional<BrandId> ownBrand = user.get().brand();
    if (selected.isEmpty() && ownBrand.isPresent()) {
      TenantContext.set(ownBrand.get());
      try {
        chain.doFilter(request, response);
      } finally {
        TenantContext.clear();
      }
      return;
    }
    chain.doFilter(request, response);
  }

  private static Optional<InternalUser> currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof InternalUserPrincipal principal) {
      return Optional.of(principal.user());
    }
    return Optional.empty();
  }
}
