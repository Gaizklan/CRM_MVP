package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.InternalUserId;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.domain.BrandId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios internos. Solo nivel ADMIN ({@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Usuarios internos", description = "Gestión de usuarios internos (solo ADMIN)")
@SecurityRequirement(name = "basicAuth")
public class InternalUserController {

  private final InternalUserService service;

  public InternalUserController(InternalUserService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Lista los usuarios internos")
  public List<InternalUserResponse> list() {
    return service.list().stream().map(InternalUserResponse::from).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Obtiene un usuario interno")
  public InternalUserResponse get(@PathVariable UUID id) {
    return InternalUserResponse.from(service.get(new InternalUserId(id)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crea un usuario interno")
  public InternalUserResponse create(@Valid @RequestBody CreateInternalUserRequest request) {
    return InternalUserResponse.from(
        service.create(
            new InternalUserCommand(
                request.username(),
                request.password(),
                request.level(),
                toBrand(request.brandId()),
                request.roles(),
                true)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifica nivel, Marca, roles, estado o contraseña de un usuario interno")
  public InternalUserResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateInternalUserRequest request) {
    return InternalUserResponse.from(
        service.update(
            new InternalUserId(id),
            new InternalUserCommand(
                null,
                request.password(),
                request.level(),
                toBrand(request.brandId()),
                request.roles(),
                request.enabled())));
  }

  private static BrandId toBrand(UUID id) {
    return id == null ? null : new BrandId(id);
  }

  public record CreateInternalUserRequest(
      @NotNull String username,
      @NotNull String password,
      @NotNull SecurityLevel level,
      UUID brandId,
      Set<FunctionalRole> roles) {}

  public record UpdateInternalUserRequest(
      @NotNull SecurityLevel level,
      UUID brandId,
      Set<FunctionalRole> roles,
      Boolean enabled,
      String password) {}

  /** Representación pública: nunca incluye el hash de la contraseña. */
  public record InternalUserResponse(
      UUID id,
      String username,
      SecurityLevel level,
      UUID brandId,
      Set<FunctionalRole> roles,
      boolean enabled) {

    static InternalUserResponse from(InternalUser user) {
      return new InternalUserResponse(
          user.id().value(),
          user.username(),
          user.level(),
          user.brand().map(BrandId::value).orElse(null),
          user.roles(),
          user.enabled());
    }
  }
}
