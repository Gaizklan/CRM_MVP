package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.example.crm.common.application.settings.BrandSettingsService;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.settings.BrandSettings;
import org.example.crm.common.domain.settings.DeliveryChannel;
import org.example.crm.common.domain.settings.NotificationSettings;
import org.example.crm.common.domain.settings.NotificationType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ajustes de negocio de la Marca seleccionada ({@code X-Brand} o {@code ?brand=}). Solo ADMIN o
 * GLOBAL_USER. Los cambios aplican inmediatamente, sin redespliegue.
 */
@RestController
@RequestMapping("/api/brand-settings")
@Tag(name = "Configuración por Marca", description = "Parámetros de negocio de la Marca")
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("hasAnyAuthority('LEVEL_ADMIN', 'LEVEL_GLOBAL_USER')")
public class BrandSettingsController {

  private final BrandSettingsService service;

  public BrandSettingsController(BrandSettingsService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Ajustes efectivos de la Marca (guardados o valores por defecto)")
  public BrandSettingsResponse get() {
    BrandId brand = TenantContext.require();
    return BrandSettingsResponse.of(service.settings(brand), service.isCustomized(brand));
  }

  @GetMapping("/defaults")
  @Operation(summary = "Valores por defecto de la Marca")
  public BrandSettingsResponse defaults() {
    return BrandSettingsResponse.of(service.defaults(TenantContext.require()), false);
  }

  @PutMapping
  @Operation(
      summary = "Sustituye los ajustes de la Marca",
      description = "Todos los campos son obligatorios; se validan rangos y formatos.")
  public BrandSettingsResponse update(
      @RequestBody BrandSettingsRequest request, Authentication authentication) {
    BrandSettings settings =
        new BrandSettings(
            request.notifications(),
            request.languages(),
            request.maxRetries(),
            request.loginLockThreshold() == null ? 0 : request.loginLockThreshold(),
            request.activeProviders());
    return BrandSettingsResponse.of(
        service.update(TenantContext.require(), settings, "admin:" + authentication.getName()),
        true);
  }

  public record BrandSettingsRequest(
      Map<NotificationType, NotificationSettings> notifications,
      List<String> languages,
      Map<DeliveryChannel, Integer> maxRetries,
      Integer loginLockThreshold,
      Map<DeliveryChannel, List<String>> activeProviders) {}

  public record BrandSettingsResponse(
      boolean customized,
      String defaultLanguage,
      Map<NotificationType, NotificationSettings> notifications,
      List<String> languages,
      Map<DeliveryChannel, Integer> maxRetries,
      int loginLockThreshold,
      Map<DeliveryChannel, List<String>> activeProviders) {

    static BrandSettingsResponse of(BrandSettings s, boolean customized) {
      return new BrandSettingsResponse(
          customized,
          s.defaultLanguage(),
          s.notifications(),
          s.languages(),
          s.maxRetries(),
          s.loginLockThreshold(),
          s.activeProviders());
    }
  }
}
