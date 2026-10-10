package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.example.crm.common.application.tenant.BrandService;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.BrandStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión del catálogo de Marcas. Solo nivel ADMIN ({@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/brands")
@Tag(name = "Marcas", description = "Gestión de Marcas (solo ADMIN)")
@SecurityRequirement(name = "basicAuth")
public class BrandController {

  private final BrandService service;

  public BrandController(BrandService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Lista las Marcas")
  public List<BrandResponse> list() {
    return service.list().stream().map(BrandResponse::from).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Obtiene una Marca")
  public BrandResponse get(@PathVariable UUID id) {
    return BrandResponse.from(service.get(new BrandId(id)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crea una Marca")
  public BrandResponse create(@Valid @RequestBody CreateBrandRequest request) {
    return BrandResponse.from(
        service.create(
            request.code(), request.name(), request.domain(), request.defaultLanguage()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifica una Marca")
  public BrandResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateBrandRequest request) {
    return BrandResponse.from(
        service.update(
            new BrandId(id),
            request.name(),
            request.domain(),
            request.defaultLanguage(),
            request.status() == BrandStatus.ACTIVE));
  }

  public record CreateBrandRequest(
      @NotNull String code, @NotNull String name, String domain, @NotNull String defaultLanguage) {}

  public record UpdateBrandRequest(
      @NotNull String name,
      String domain,
      @NotNull String defaultLanguage,
      @NotNull BrandStatus status) {}

  public record BrandResponse(
      UUID id,
      String code,
      String name,
      String domain,
      String defaultLanguage,
      BrandStatus status) {

    static BrandResponse from(Brand brand) {
      return new BrandResponse(
          brand.id().value(),
          brand.code(),
          brand.name(),
          brand.domain().orElse(null),
          brand.defaultLanguage(),
          brand.status());
    }
  }
}
