package org.example.crm.common.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.example.crm.common.application.DomainEventHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class EventsConfiguration {

  @Bean
  OutboxEventSerializer outboxEventSerializer(ObjectMapper mapper) {
    return new OutboxEventSerializer(mapper);
  }

  @Bean
  TransactionalOutboxPublisher domainEventPublisher(
      JdbcTemplate jdbc, OutboxEventSerializer serializer, ApplicationEventPublisher spring) {
    return new TransactionalOutboxPublisher(jdbc, serializer, spring);
  }

  @Bean
  OutboxRelay outboxRelay(
      JdbcTemplate jdbc,
      TransactionTemplate tx,
      OutboxEventSerializer serializer,
      List<DomainEventHandler<?>> handlers,
      @Value("${crm.outbox.relay.batch-size:100}") int batchSize,
      @Value("${crm.outbox.relay.max-attempts:10}") int maxAttempts) {
    return new OutboxRelay(jdbc, tx, serializer, handlers, batchSize, maxAttempts);
  }

  /** Ejecución periódica del relay (desactivable con {@code crm.outbox.relay.enabled=false}). */
  @Configuration
  @EnableScheduling
  @ConditionalOnProperty(
      name = "crm.outbox.relay.enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class OutboxRelayScheduling {

    private final OutboxRelay relay;

    OutboxRelayScheduling(OutboxRelay relay) {
      this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${crm.outbox.relay.interval:PT1S}")
    void relay() {
      relay.relayPending();
    }
  }
}
