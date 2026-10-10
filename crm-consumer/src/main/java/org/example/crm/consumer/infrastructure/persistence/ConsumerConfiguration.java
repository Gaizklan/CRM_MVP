package org.example.crm.consumer.infrastructure.persistence;

import java.time.Clock;
import org.example.crm.common.application.DomainEventPublisher;
import org.example.crm.consumer.application.ConsumerRepository;
import org.example.crm.consumer.application.ConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerConfiguration {

  @Bean
  ConsumerService consumerService(
      ConsumerRepository consumers, DomainEventPublisher events, Clock clock) {
    return new ConsumerService(consumers, events, clock);
  }
}
