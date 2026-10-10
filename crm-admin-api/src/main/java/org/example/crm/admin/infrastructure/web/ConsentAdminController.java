package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.domain.ConsentChange;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consents;
import org.example.crm.consumer.domain.ConsumerId;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta y cambio administrativo de consentimientos (origen ADMIN, actor = usuario interno). */
@RestController
@RequestMapping("/api/consumers/{consumerId}/consents")
@Tag(name = "Consentimientos", description = "Opt-in/opt-out por canal con registro auditable")
@SecurityRequirement(name = "basicAuth")
public class ConsentAdminController {

  private final ConsentService service;

  public ConsentAdminController(ConsentService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Consentimientos vigentes y canales permitidos")
  public ConsentsResponse current(@PathVariable UUID consumerId) {
    return ConsentsResponse.from(service.current(new ConsumerId(consumerId)));
  }

  @GetMapping("/history")
  @Operation(summary = "Registro de auditoría de consentimientos (más reciente primero)")
  public List<ConsentChangeResponse> history(@PathVariable UUID consumerId) {
    return service.history(new ConsumerId(consumerId)).stream()
        .map(ConsentChangeResponse::from)
        .toList();
  }

  @PutMapping("/{channel}")
  @Operation(
      summary = "Concede o retira el consentimiento de un canal",
      description = "El opt-out de CRM_PROGRAM implica el opt-out de todos los canales.")
  public ConsentsResponse change(
      @PathVariable UUID consumerId,
      @PathVariable ConsentChannel channel,
      @Valid @RequestBody ChangeConsentRequest request,
      Authentication authentication) {
    return ConsentsResponse.from(
        service.change(
            new ConsumerId(consumerId),
            channel,
            request.granted(),
            ConsentSource.ADMIN,
            "admin:" + authentication.getName()));
  }

  public record ChangeConsentRequest(@NotNull Boolean granted) {}

  public record ChannelConsent(
      boolean granted, boolean allowed, ConsentSource source, Instant since) {}

  public record ConsentsResponse(Map<ConsentChannel, ChannelConsent> channels) {
    static ConsentsResponse from(Consents consents) {
      Map<ConsentChannel, ChannelConsent> map = new EnumMap<>(ConsentChannel.class);
      for (ConsentChannel channel : ConsentChannel.values()) {
        var status = consents.status(channel);
        map.put(
            channel,
            new ChannelConsent(
                consents.isGranted(channel),
                consents.isAllowed(channel),
                status.map(s -> s.source()).orElse(null),
                status.map(s -> s.since()).orElse(null)));
      }
      return new ConsentsResponse(map);
    }
  }

  public record ConsentChangeResponse(
      ConsentChannel channel,
      boolean granted,
      ConsentSource source,
      String actor,
      Instant occurredAt) {
    static ConsentChangeResponse from(ConsentChange c) {
      return new ConsentChangeResponse(
          c.channel(), c.granted(), c.source(), c.actor(), c.occurredAt());
    }
  }
}
