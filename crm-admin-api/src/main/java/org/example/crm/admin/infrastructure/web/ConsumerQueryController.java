package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.ConsumerStatus;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.PreferenceChange;
import org.example.crm.consumer.domain.PreferenceType;
import org.example.crm.consumer.domain.PreferenceValue;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta administrativa de consumidores de la Marca seleccionada (cabecera {@code X-Brand} o
 * parámetro {@code brand}; un LOCAL_USER usa su Marca).
 */
@RestController
@RequestMapping("/api/consumers")
@Tag(name = "Consumidores", description = "Consulta de consumidores de la Marca seleccionada")
@SecurityRequirement(name = "basicAuth")
public class ConsumerQueryController {

  private final ConsumerService service;

  public ConsumerQueryController(ConsumerService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Busca consumidores con filtros y paginación")
  public PageResult<ConsumerResponse> search(
      @Parameter(description = "Email exacto") @RequestParam(required = false) String email,
      @RequestParam(required = false) ConsumerStatus status,
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String segment,
      @Parameter(description = "Alta desde (inclusive), ISO-8601")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant registeredFrom,
      @Parameter(description = "Alta hasta (exclusive), ISO-8601")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant registeredTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    ConsumerSearchCriteria criteria =
        new ConsumerSearchCriteria(
            email == null || email.isBlank() ? null : new Email(email),
            status,
            language == null || language.isBlank() ? null : new LanguageTag(language),
            segment == null || segment.isBlank() ? null : segment,
            registeredFrom,
            registeredTo);
    return service.search(criteria, new PageQuery(page, size)).map(ConsumerResponse::from);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Obtiene un consumidor")
  public ConsumerResponse get(@PathVariable UUID id) {
    return ConsumerResponse.from(service.get(new ConsumerId(id)));
  }

  @GetMapping("/{id}/preference-history")
  @Operation(summary = "Historial de cambios de preferencias (más reciente primero)")
  public List<PreferenceChangeResponse> preferenceHistory(@PathVariable UUID id) {
    return service.preferenceHistory(new ConsumerId(id)).stream()
        .map(PreferenceChangeResponse::from)
        .toList();
  }

  public record PreferenceResponse(PreferenceType type, String value) {
    static PreferenceResponse from(PreferenceValue value) {
      return value == null ? null : new PreferenceResponse(value.type(), value.asText());
    }
  }

  public record ConsumerResponse(
      UUID id,
      String email,
      String phone,
      String language,
      String segment,
      ConsumerStatus status,
      Instant registeredAt,
      Map<String, String> attributes,
      Map<String, PreferenceResponse> preferences) {

    static ConsumerResponse from(Consumer c) {
      return new ConsumerResponse(
          c.id().value(),
          c.email().value(),
          c.phone().map(PhoneNumber::value).orElse(null),
          c.language().value(),
          c.segment().orElse(null),
          c.status(),
          c.registeredAt(),
          c.attributes(),
          c.preferences().entrySet().stream()
              .collect(
                  Collectors.toMap(Map.Entry::getKey, e -> PreferenceResponse.from(e.getValue()))));
    }
  }

  public record PreferenceChangeResponse(
      String key,
      PreferenceResponse oldValue,
      PreferenceResponse newValue,
      String changedBy,
      Instant changedAt) {

    static PreferenceChangeResponse from(PreferenceChange c) {
      return new PreferenceChangeResponse(
          c.key(),
          PreferenceResponse.from(c.oldValue()),
          PreferenceResponse.from(c.newValue()),
          c.changedBy(),
          c.changedAt());
    }
  }
}
