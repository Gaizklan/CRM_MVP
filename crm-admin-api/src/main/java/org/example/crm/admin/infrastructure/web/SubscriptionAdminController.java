package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.crm.consumer.application.SubscriptionManager;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.Subscription;
import org.example.crm.consumer.domain.SubscriptionService;
import org.example.crm.consumer.domain.SubscriptionServiceId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de servicios suscribibles de la Marca y suscripciones de consumidores. */
@RestController
@Tag(name = "Suscripciones", description = "Servicios del programa y suscripciones")
@SecurityRequirement(name = "basicAuth")
public class SubscriptionAdminController {

  private final SubscriptionManager manager;

  public SubscriptionAdminController(SubscriptionManager manager) {
    this.manager = manager;
  }

  @GetMapping("/api/subscription-services")
  @Operation(summary = "Lista los servicios suscribibles de la Marca")
  public List<ServiceResponse> services() {
    return manager.services().stream().map(ServiceResponse::from).toList();
  }

  @PostMapping("/api/subscription-services")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crea un servicio suscribible en la Marca")
  public ServiceResponse createService(@Valid @RequestBody CreateServiceRequest request) {
    return ServiceResponse.from(
        manager.createService(request.code(), request.name(), request.description()));
  }

  @PutMapping("/api/subscription-services/{id}")
  @Operation(summary = "Modifica un servicio suscribible")
  public ServiceResponse updateService(
      @PathVariable UUID id, @Valid @RequestBody UpdateServiceRequest request) {
    return ServiceResponse.from(
        manager.updateService(
            new SubscriptionServiceId(id),
            request.name(),
            request.description(),
            request.active()));
  }

  @GetMapping("/api/consumers/{consumerId}/subscriptions")
  @Operation(summary = "Suscripciones (activas e históricas) del consumidor")
  public List<SubscriptionResponse> subscriptions(@PathVariable UUID consumerId) {
    return manager.subscriptions(new ConsumerId(consumerId)).stream()
        .map(SubscriptionResponse::from)
        .toList();
  }

  @PutMapping("/api/consumers/{consumerId}/subscriptions/{serviceCode}")
  @Operation(
      summary = "Suscribe al consumidor a un servicio",
      description = "Requiere opt-in vigente del canal SUBSCRIPTIONS (y del programa CRM).")
  public SubscriptionResponse subscribe(
      @PathVariable UUID consumerId, @PathVariable String serviceCode) {
    return SubscriptionResponse.from(manager.subscribe(new ConsumerId(consumerId), serviceCode));
  }

  @DeleteMapping("/api/consumers/{consumerId}/subscriptions/{serviceCode}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Da de baja la suscripción activa del consumidor")
  public void unsubscribe(@PathVariable UUID consumerId, @PathVariable String serviceCode) {
    manager.unsubscribe(new ConsumerId(consumerId), serviceCode);
  }

  public record CreateServiceRequest(
      @NotNull String code, @NotNull String name, String description) {}

  public record UpdateServiceRequest(
      @NotNull String name, String description, @NotNull Boolean active) {}

  public record ServiceResponse(
      UUID id, String code, String name, String description, boolean active) {
    static ServiceResponse from(SubscriptionService s) {
      return new ServiceResponse(s.id().value(), s.code(), s.name(), s.description(), s.active());
    }
  }

  public record SubscriptionResponse(
      UUID id, UUID serviceId, Instant subscribedAt, Instant unsubscribedAt, boolean active) {
    static SubscriptionResponse from(Subscription s) {
      return new SubscriptionResponse(
          s.id(),
          s.serviceId().value(),
          s.subscribedAt(),
          s.unsubscribedAt().orElse(null),
          s.isActive());
    }
  }
}
