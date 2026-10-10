package org.example.crm.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.application.ConsentChecker;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ConsentIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired ConsumerService consumers;
  @Autowired ConsentService consents;
  @Autowired ConsentChecker checker;
  @Autowired BrandLookup brands;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;

  BrandId brandA;
  Consumer consumer;

  @BeforeEach
  void setUp() {
    brandA = brands.findByCode("marca-a").orElseThrow().id();
    if (userRepository.findByUsername("gestor.consent").isEmpty()) {
      users.create(
          new InternalUserCommand(
              "gestor.consent", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
    }
    consumer =
        TenantContext.callAs(
            brandA,
            () ->
                consumers.register(
                    new Email("consent-" + System.nanoTime() + "@example.test"),
                    null,
                    new LanguageTag("es")));
  }

  @Test
  void eachChannelIsCheckedAndAudited() {
    TenantContext.runAs(
        brandA,
        () -> {
          consents.change(
              consumer.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer");
          for (ConsentChannel channel : ConsentChannel.values()) {
            if (channel.isProgram()) {
              continue;
            }
            assertThat(checker.isAllowed(consumer.id(), channel)).isFalse();
            consents.change(consumer.id(), channel, true, ConsentSource.ADMIN, "admin:test");
            assertThat(checker.isAllowed(consumer.id(), channel)).isTrue();
          }
          consents.change(
              consumer.id(), ConsentChannel.CRM_PROGRAM, false, ConsentSource.EMAIL, "consumer");
          for (ConsentChannel channel : ConsentChannel.values()) {
            assertThat(checker.isAllowed(consumer.id(), channel)).as(channel.name()).isFalse();
          }
          var history = consents.history(consumer.id());
          assertThat(history).hasSize(1 + 5 + 1 + 5);
          assertThat(history.get(0).actor()).isEqualTo("consumer");
          assertThat(history.get(0).source()).isEqualTo(ConsentSource.EMAIL);
        });
    assertThat(checker.isAllowed(consumer.id(), ConsentChannel.EMAIL))
        .as("sin Marca en contexto")
        .isFalse();
  }

  @Test
  void auditLogIsImmutableInDatabase() {
    TenantContext.runAs(
        brandA,
        () ->
            consents.change(
                consumer.id(), ConsentChannel.SMS, true, ConsentSource.IMPORT, "import:1"));

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update consumer_consent_log set actor = 'x' where consumer_id = ?",
                    consumer.id().value()))
        .hasMessageContaining("inmutable");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "delete from consumer_consent_log where consumer_id = ?",
                    consumer.id().value()))
        .hasMessageContaining("inmutable");
  }

  @Test
  void adminApiChangesConsentWithAdminSourceAndActor() throws Exception {
    mvc.perform(
            put("/api/consumers/" + consumer.id() + "/consents/CRM_PROGRAM")
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.consent", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"granted\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.channels.CRM_PROGRAM.allowed").value(true))
        .andExpect(jsonPath("$.channels.EMAIL.allowed").value(false));
    mvc.perform(
            get("/api/consumers/" + consumer.id() + "/consents/history")
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.consent", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("ADMIN"))
        .andExpect(jsonPath("$[0].actor").value("admin:gestor.consent"));
    mvc.perform(
            get("/api/consumers/" + consumer.id() + "/consents")
                .header("X-Brand", "marca-b")
                .with(httpBasic("gestor.consent", PASSWORD)))
        .andExpect(status().isNotFound());
  }
}
