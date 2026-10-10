package org.example.crm.tracking.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.example.crm.common.application.DomainEventHandler;
import org.example.crm.consumer.domain.ConsentChanged;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.tracking.application.PreferenceEventsHandler;
import org.example.crm.tracking.application.UserEventRecorder;
import org.example.crm.tracking.application.UserEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class TrackingConfiguration {

  @Bean
  UserEventRepository userEventRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    return new JdbcUserEventRepository(jdbc, mapper);
  }

  /** También es el {@code UserEventLookup} del motor de reglas ({@code eventOccurred}). */
  @Bean
  UserEventRecorder userEventRecorder(UserEventRepository repository, Clock clock) {
    return new UserEventRecorder(repository, clock);
  }

  @Bean
  DomainEventHandler<ConsumerProfileChanged> preferenceChangesToUserEvents(
      UserEventRecorder recorder) {
    return PreferenceEventsHandler.profileChanges(recorder);
  }

  @Bean
  DomainEventHandler<ConsentChanged> consentChangesToUserEvents(UserEventRecorder recorder) {
    return PreferenceEventsHandler.consentChanges(recorder);
  }
}
