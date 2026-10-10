package org.example.crm.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.example.crm.common.domain.DomainException;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.application.SubscriptionManager;
import org.example.crm.consumer.application.SubscriptionQuery;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SubscriptionIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired ConsumerService consumers;
  @Autowired ConsentService consents;
  @Autowired SubscriptionManager subscriptions;
  @Autowired SubscriptionQuery query;
  @Autowired BrandLookup brands;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired MockMvc mvc;

  BrandId brandA;
  Consumer consumer;

  @BeforeEach
  void setUp() {
    brandA = brands.findByCode("marca-a").orElseThrow().id();
    if (userRepository.findByUsername("gestor.subs").isEmpty()) {
      users.create(
          new InternalUserCommand(
              "gestor.subs", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
    }
    consumer =
        TenantContext.callAs(
            brandA,
            () ->
                consumers.register(
                    new Email("subs-" + System.nanoTime() + "@example.test"),
                    null,
                    new LanguageTag("es")));
  }

  @Test
  void subscribeRequiresOptInAndFeedsRulesQuery() {
    TenantContext.runAs(
        brandA,
        () -> {
          assertThat(subscriptions.services())
              .extracting(s -> s.code())
              .contains("newsletter", "news");
          assertThatThrownBy(() -> subscriptions.subscribe(consumer.id(), "newsletter"))
              .isInstanceOf(DomainException.class);
          consents.change(consumer.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "c");
          consents.change(
              consumer.id(), ConsentChannel.SUBSCRIPTIONS, true, ConsentSource.WEB, "c");
          subscriptions.subscribe(consumer.id(), "newsletter");
          subscriptions.subscribe(consumer.id(), "news");
          subscriptions.unsubscribe(consumer.id(), "news");

          assertThat(query.activeServiceCodes(consumer.id())).containsExactly("newsletter");
          assertThat(subscriptions.subscriptions(consumer.id()))
              .hasSize(2)
              .anySatisfy(s -> assertThat(s.unsubscribedAt()).isPresent());
        });
  }

  @Test
  void adminApi() throws Exception {
    TenantContext.runAs(
        brandA,
        () -> {
          consents.change(consumer.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "c");
          consents.change(
              consumer.id(), ConsentChannel.SUBSCRIPTIONS, true, ConsentSource.WEB, "c");
        });
    String base = "/api/consumers/" + consumer.id() + "/subscriptions";

    mvc.perform(
            put(base + "/newsletter")
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.subs", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(
            put(base + "/newsletter")
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.subs", PASSWORD)))
        .andExpect(status().isConflict());
    mvc.perform(
            delete(base + "/newsletter")
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.subs", PASSWORD)))
        .andExpect(status().isNoContent());
    mvc.perform(get(base).header("X-Brand", "marca-a").with(httpBasic("gestor.subs", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(false))
        .andExpect(jsonPath("$[0].unsubscribedAt").isNotEmpty());
    mvc.perform(
            get("/api/subscription-services")
                .header("X-Brand", "marca-b")
                .with(httpBasic("gestor.subs", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }
}
