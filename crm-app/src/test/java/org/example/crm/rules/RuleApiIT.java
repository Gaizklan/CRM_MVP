package org.example.crm.rules;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.consumer.application.ConsentService;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.ProfileUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RuleApiIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mvc;
  @Autowired ConsumerService consumers;
  @Autowired ConsentService consents;
  @Autowired BrandLookup brands;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;

  @Test
  void validatesAndEvaluatesRules() throws Exception {
    if (userRepository.findByUsername("gestor.rules").isEmpty()) {
      users.create(
          new InternalUserCommand(
              "gestor.rules", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
    }
    var brandA = brands.findByCode("marca-a").orElseThrow().id();
    Consumer c =
        TenantContext.callAs(
            brandA,
            () -> {
              Consumer x =
                  consumers.register(new Email("rules@example.test"), null, new LanguageTag("es"));
              consumers.updateProfile(
                  x.id(), new ProfileUpdate(null, null, null, Optional.of("S1"), null));
              consents.change(x.id(), ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "c");
              consents.change(x.id(), ConsentChannel.EMAIL, true, ConsentSource.WEB, "c");
              return x;
            });

    mvc.perform(
            post("/api/rules/validate")
                .with(httpBasic("gestor.rules", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gt\":{\"field\":\"segment\",\"value\":1}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.startsWith("$.gt.value")));
    mvc.perform(
            post("/api/rules/evaluate/" + c.id())
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.rules", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"and":[{"eq":{"field":"segment","value":"S1"}},
                            {"eq":{"field":"consent.EMAIL","value":true}}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matches").value(true));
    mvc.perform(
            post("/api/rules/evaluate/" + c.id())
                .header("X-Brand", "marca-a")
                .with(httpBasic("gestor.rules", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foo\":1}"))
        .andExpect(status().isBadRequest());
  }
}
