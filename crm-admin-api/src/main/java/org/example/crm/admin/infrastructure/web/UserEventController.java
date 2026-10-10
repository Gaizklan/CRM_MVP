package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.PageResult;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.tracking.application.UserEventRecorder;
import org.example.crm.tracking.domain.UserEvent;
import org.example.crm.tracking.domain.UserEventCriteria;
import org.example.crm.tracking.domain.UserEventType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de eventos de usuario de la Marca seleccionada. */
@RestController
@Tag(name = "Eventos de usuario", description = "Consulta de eventos por consumidor, tipo y fechas")
@SecurityRequirement(name = "basicAuth")
public class UserEventController {

  private final UserEventRecorder recorder;

  public UserEventController(UserEventRecorder recorder) {
    this.recorder = recorder;
  }

  @GetMapping("/api/user-events")
  @Operation(summary = "Busca eventos de usuario (más recientes primero)")
  public PageResult<UserEventResponse> search(
      @RequestParam(required = false) UUID consumerId,
      @RequestParam(required = false) UserEventType type,
      @Parameter(description = "Desde (inclusive), ISO-8601")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @Parameter(description = "Hasta (exclusive), ISO-8601")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return recorder
        .search(
            new UserEventCriteria(
                consumerId == null ? null : new ConsumerId(consumerId), type, from, to),
            new PageQuery(page, size))
        .map(UserEventResponse::from);
  }

  public record UserEventResponse(
      UUID id,
      UUID consumerId,
      UserEventType type,
      Instant occurredAt,
      Map<String, String> attributes) {
    static UserEventResponse from(UserEvent e) {
      return new UserEventResponse(
          e.id(), e.consumerId().value(), e.type(), e.occurredAt(), e.attributes());
    }
  }
}
