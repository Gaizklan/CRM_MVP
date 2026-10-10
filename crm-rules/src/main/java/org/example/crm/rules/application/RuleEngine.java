package org.example.crm.rules.application;

import java.time.Clock;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.example.crm.consumer.application.ConsentChecker;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.application.SubscriptionQuery;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.rules.domain.Rule;
import org.example.crm.rules.domain.RuleContext;
import org.example.crm.rules.domain.RuleEvaluator;
import org.example.crm.rules.domain.RuleParser;

/**
 * Puerto de entrada del motor de reglas: parseo/validación del DSL JSON y evaluación en memoria
 * contra un consumidor de la Marca actual. Reutilizado por poblaciones, condiciones de workflow,
 * personalización de contenidos y menús.
 */
public class RuleEngine {

  private final RuleReader reader;
  private final ConsumerService consumers;
  private final ConsentChecker consents;
  private final SubscriptionQuery subscriptions;
  private final UserEventLookup events;
  private final Clock clock;

  public RuleEngine(
      RuleReader reader,
      ConsumerService consumers,
      ConsentChecker consents,
      SubscriptionQuery subscriptions,
      UserEventLookup events,
      Clock clock) {
    this.reader = reader;
    this.consumers = consumers;
    this.consents = consents;
    this.subscriptions = subscriptions;
    this.events = events;
    this.clock = clock;
  }

  /** Parsea y valida una regla JSON. */
  public Rule parse(String json) {
    return RuleParser.parse(reader.read(json));
  }

  /** Errores de validación de una regla JSON (vacío si es válida). */
  public List<String> validate(String json) {
    try {
      return RuleParser.validate(reader.read(json));
    } catch (org.example.crm.rules.domain.RuleValidationException e) {
      return e.errors();
    }
  }

  /** Evalúa la regla para un consumidor de la Marca actual. */
  public boolean evaluate(Rule rule, ConsumerId consumerId) {
    return RuleEvaluator.evaluate(rule, contextFor(consumerId));
  }

  /** Construye el {@link RuleContext} de un consumidor de la Marca actual. */
  public RuleContext contextFor(ConsumerId consumerId) {
    Consumer c = consumers.get(consumerId);
    Map<String, Object> profile = new HashMap<>();
    profile.put("email", c.email().value());
    c.phone().map(PhoneNumber::value).ifPresent(p -> profile.put("phone", p));
    profile.put("language", c.language().value());
    c.segment().ifPresent(s -> profile.put("segment", s));
    profile.put("status", c.status().name());
    profile.put("registeredAt", c.registeredAt());
    Set<ConsentChannel> allowed = EnumSet.noneOf(ConsentChannel.class);
    for (ConsentChannel channel : ConsentChannel.values()) {
      if (consents.isAllowed(consumerId, channel)) {
        allowed.add(channel);
      }
    }
    return new RuleContext(
        consumerId,
        profile,
        c.attributes(),
        c.preferences(),
        allowed,
        subscriptions.activeServiceCodes(consumerId),
        (type, since) -> events.occurredSince(consumerId, type, since),
        clock.instant());
  }
}
