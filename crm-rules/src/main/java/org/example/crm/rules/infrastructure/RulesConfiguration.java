package org.example.crm.rules.infrastructure;

import java.time.Clock;
import org.example.crm.consumer.application.ConsentChecker;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.application.SubscriptionQuery;
import org.example.crm.rules.application.RuleEngine;
import org.example.crm.rules.application.RuleReader;
import org.example.crm.rules.application.UserEventLookup;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RulesConfiguration {

  @Bean
  RuleReader ruleReader() {
    return new JacksonRuleReader();
  }

  @Bean
  RuleEngine ruleEngine(
      RuleReader reader,
      ConsumerService consumers,
      ConsentChecker consents,
      SubscriptionQuery subscriptions,
      ObjectProvider<UserEventLookup> events,
      Clock clock) {
    // Sin módulo de tracking (P11) no hay eventos: eventOccurred es siempre falso.
    UserEventLookup lookup = events.getIfAvailable(() -> (consumer, type, since) -> false);
    return new RuleEngine(reader, consumers, consents, subscriptions, lookup, clock);
  }
}
