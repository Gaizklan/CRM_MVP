package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.infrastructure.security.InternalUserPrincipal;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos del usuario interno autenticado y Marca efectiva de la petición. */
@RestController
@Tag(name = "Sesión", description = "Usuario interno autenticado")
@SecurityRequirement(name = "basicAuth")
public class MeController {

  @GetMapping("/api/me")
  @Operation(summary = "Usuario autenticado, autoridades y Marca efectiva")
  public MeResponse me(@AuthenticationPrincipal InternalUserPrincipal principal) {
    InternalUser user = principal.user();
    return new MeResponse(
        user.username(),
        user.level().name(),
        user.brand().map(BrandId::value).orElse(null),
        principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
        TenantContext.current().map(BrandId::value).orElse(null));
  }

  public record MeResponse(
      String username, String level, UUID brandId, List<String> authorities, UUID currentBrandId) {}
}
