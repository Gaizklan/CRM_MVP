package org.example.crm.consumer.infrastructure.persistence;

import java.time.Clock;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.consumer.application.ConsentRepository;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerRepository;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.application.SubscriptionManager;
import org.example.crm.consumer.application.SubscriptionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerConfiguration {

  @Bean
  ConsumerService consumerService(
      ConsumerRepository consumers, DomainEventPublisher events, Clock clock) {
    return new ConsumerService(consumers, events, clock);
  }

  @Bean
  ConsentService consentService(
      ConsumerService consumers,
      ConsentRepository consents,
      DomainEventPublisher events,
      Clock clock) {
    return new ConsentService(consumers, consents, events, clock);
  }

  @Bean
  SubscriptionManager subscriptionManager(
      SubscriptionRepository subscriptions,
      ConsumerService consumers,
      ConsentService consents,
      Clock clock) {
    return new SubscriptionManager(subscriptions, consumers, consents, clock);
  }
}
