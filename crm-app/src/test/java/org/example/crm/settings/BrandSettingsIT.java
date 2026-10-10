package org.example.crm.settings;

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
import org.junit.jupiter.api.BeforeEach;
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
class BrandSettingsIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mvc;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired BrandLookup brands;

  static final String BODY =
      """
{
  "notifications": {
    "REGISTRATION_VERIFICATION": {"senderName":"Marca B","senderAddress":"hola@marca-b.example.test","subject":"Bienvenida"},
    "PASSWORD_RESET": {"senderName":"Marca B","senderAddress":"hola@marca-b.example.test","subject":"Contraseña"},
    "ACCOUNT_LOCKED": {"senderName":"Marca B","senderAddress":"hola@marca-b.example.test","subject":"Bloqueo"},
    "UNSUBSCRIBE_CONFIRMATION": {"senderName":"Marca B","senderAddress":"hola@marca-b.example.test","subject":"Baja"}
  },
  "languages": ["es", "en"],
  "maxRetries": {"EMAIL": 4, "SMS": 2, "MMS": 1, "PUSH": 1, "INBOX": 0},
  "loginLockThreshold": %d,
  "activeProviders": {"EMAIL": ["smtp-1"], "SMS": ["gateway-1","gateway-2"], "MMS": [], "PUSH": [], "INBOX": []}
}
""";

  @BeforeEach
  void users() {
    if (userRepository.findByUsername("settings.global").isEmpty()) {
      var brandB = brands.findByCode("marca-b").orElseThrow().id();
      users.create(
          new InternalUserCommand(
              "settings.global", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
      users.create(
          new InternalUserCommand(
              "settings.admin", PASSWORD, SecurityLevel.ADMIN, null, Set.of(), true));
      users.create(
          new InternalUserCommand(
              "settings.local", PASSWORD, SecurityLevel.LOCAL_USER, brandB, Set.of(), true));
    }
  }

  @Test
  void defaultsThenUpdateWithoutRedeploy() throws Exception {
    mvc.perform(
            get("/api/brand-settings/defaults")
                .header("X-Brand", "marca-a")
                .with(httpBasic("settings.admin", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginLockThreshold").value(5))
        .andExpect(jsonPath("$.maxRetries.EMAIL").value(3))
        .andExpect(
            jsonPath("$.notifications.PASSWORD_RESET.senderAddress")
                .value("no-reply@marca-a.localhost"));

    mvc.perform(
            put("/api/brand-settings")
                .header("X-Brand", "marca-b")
                .with(httpBasic("settings.global", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customized").value(true));

    mvc.perform(
            get("/api/brand-settings")
                .header("X-Brand", "marca-b")
                .with(httpBasic("settings.admin", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customized").value(true))
        .andExpect(jsonPath("$.loginLockThreshold").value(3))
        .andExpect(jsonPath("$.maxRetries.EMAIL").value(4))
        .andExpect(jsonPath("$.activeProviders.SMS[1]").value("gateway-2"))
        .andExpect(jsonPath("$.languages[1]").value("en"))
        .andExpect(
            jsonPath("$.notifications.REGISTRATION_VERIFICATION.subject").value("Bienvenida"));
    mvc.perform(
            get("/api/brand-settings")
                .header("X-Brand", "marca-a")
                .with(httpBasic("settings.admin", PASSWORD)))
        .andExpect(jsonPath("$.customized").value(false));
  }

  @Test
  void validationErrorsAreReported() throws Exception {
    mvc.perform(
            put("/api/brand-settings")
                .header("X-Brand", "marca-b")
                .with(httpBasic("settings.admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(50)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("umbral")));
    mvc.perform(
            put("/api/brand-settings")
                .header("X-Brand", "marca-b")
                .with(httpBasic("settings.admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(3).replace("\"es\", \"en\"", "\"en\"")))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("idioma por defecto")));
  }

  @Test
  void onlyAdminOrGlobalUser() throws Exception {
    mvc.perform(get("/api/brand-settings").with(httpBasic("settings.local", PASSWORD)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/brand-settings")
                .with(httpBasic("settings.local", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(3)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/brand-settings").with(httpBasic("settings.admin", PASSWORD)))
        .andExpect(status().isBadRequest());
  }
}
