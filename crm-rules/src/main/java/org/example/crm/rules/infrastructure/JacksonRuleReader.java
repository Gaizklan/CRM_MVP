package org.example.crm.rules.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.example.crm.rules.application.RuleReader;
import org.example.crm.rules.domain.RuleValidationException;

/** Lee JSON con Jackson; los números se leen como {@code BigDecimal} para no perder precisión. */
public class JacksonRuleReader implements RuleReader {

  private final ObjectMapper mapper =
      JsonMapper.builder()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  @Override
  public Object read(String json) {
    if (json == null || json.isBlank()) {
      throw new RuleValidationException(List.of("$: la regla está vacía"));
    }
    try {
      return mapper.readValue(json, Object.class);
    } catch (JsonProcessingException e) {
      throw new RuleValidationException(List.of("$: JSON no válido: " + e.getOriginalMessage()));
    }
  }
}
