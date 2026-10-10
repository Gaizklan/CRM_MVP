package org.example.crm.admin.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.rules.application.RuleEngine;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Validación y prueba de reglas de negocio (DSL JSON). */
@RestController
@RequestMapping("/api/rules")
@Tag(name = "Reglas", description = "Validación y evaluación del DSL de reglas de negocio")
@SecurityRequirement(name = "basicAuth")
public class RuleController {

  private final RuleEngine engine;

  public RuleController(RuleEngine engine) {
    this.engine = engine;
  }

  @PostMapping("/validate")
  @Operation(summary = "Valida una regla y devuelve los errores encontrados")
  public ValidationResponse validate(@RequestBody JsonNode rule) {
    List<String> errors = engine.validate(rule.toString());
    return new ValidationResponse(errors.isEmpty(), errors);
  }

  @PostMapping("/evaluate/{consumerId}")
  @Operation(summary = "Evalúa una regla en memoria para un consumidor de la Marca seleccionada")
  public EvaluationResponse evaluate(@PathVariable UUID consumerId, @RequestBody JsonNode rule) {
    return new EvaluationResponse(
        engine.evaluate(engine.parse(rule.toString()), new ConsumerId(consumerId)));
  }

  public record ValidationResponse(boolean valid, List<String> errors) {}

  public record EvaluationResponse(boolean matches) {}
}
