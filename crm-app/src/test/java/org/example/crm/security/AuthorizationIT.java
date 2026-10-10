package org.example.crm.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.domain.BrandId;
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

/** Autorización de usuarios internos por nivel (ADMIN/GLOBAL_USER/LOCAL_USER) y rol funcional. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthorizationIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired MockMvc mvc;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired BrandLookup brands;

  BrandId brandA;
  BrandId brandB;

  @BeforeEach
  void createUsers() {
    brandA = brands.findByCode("marca-a").orElseThrow().id();
    brandB = brands.findByCode("marca-b").orElseThrow().id();
    ensure("admin", SecurityLevel.ADMIN, null, Set.of());
    ensure("global", SecurityLevel.GLOBAL_USER, null, Set.of(FunctionalRole.CRM_MANAGER));
    ensure("local.a", SecurityLevel.LOCAL_USER, brandA, Set.of(FunctionalRole.PRODUCER));
  }

  private void ensure(
      String username, SecurityLevel level, BrandId brand, Set<FunctionalRole> roles) {
    if (userRepository.findByUsername(username).isEmpty()) {
      users.create(new InternalUserCommand(username, PASSWORD, level, brand, roles, true));
    }
  }

  @Test
  void apiRequiresAuthentication() throws Exception {
    mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/me").with(httpBasic("admin", "mala-clave-000000")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void formLoginForInternalUsers() throws Exception {
    mvc.perform(formLogin().user("global").password(PASSWORD)).andExpect(authenticated());
    mvc.perform(formLogin().user("global").password("incorrecta-000000"))
        .andExpect(unauthenticated());
  }

  @Test
  void onlyAdminManagesUsersAndBrands() throws Exception {
    mvc.perform(get("/api/admin/users").with(httpBasic("admin", PASSWORD)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/admin/brands").with(httpBasic("admin", PASSWORD)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/admin/users").with(httpBasic("global", PASSWORD)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/brands").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesUserWithoutExposingPassword() throws Exception {
    String body =
        """
{"username":"nuevo.local.b","password":"%s","level":"LOCAL_USER","brandId":"%s","roles":["DEVELOPER"]}
"""
            .formatted(PASSWORD, brandB);
    mvc.perform(
            post("/api/admin/users")
                .with(httpBasic("admin", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.level").value("LOCAL_USER"))
        .andExpect(jsonPath("$.brandId").value(brandB.toString()))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    mvc.perform(
            post("/api/admin/users")
                .with(httpBasic("global", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("nuevo.local.b", "otro.local.b")))
        .andExpect(status().isForbidden());
  }

  @Test
  void localUserOperatesOnlyOnItsBrand() throws Exception {
    mvc.perform(get("/api/me").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentBrandId").value(brandA.toString()));
    mvc.perform(get("/api/me").header("X-Brand", "marca-a").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/me").header("X-Brand", "marca-b").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isForbidden());
  }

  @Test
  void globalAndAdminOperateOnAnyBrand() throws Exception {
    mvc.perform(get("/api/me").header("X-Brand", "marca-b").with(httpBasic("global", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentBrandId").value(brandB.toString()));
    mvc.perform(get("/api/me").param("brand", "marca-a").with(httpBasic("admin", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentBrandId").value(brandA.toString()));
  }

  @Test
  void functionalRolesAreEnforced() throws Exception {
    mvc.perform(get("/api/test/producer").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/test/reviewer").with(httpBasic("local.a", PASSWORD)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/test/producer").with(httpBasic("global", PASSWORD)))
        .andExpect(status().isForbidden());
  }

  @Test
  void disabledUserCannotLogIn() throws Exception {
    ensure("baja", SecurityLevel.GLOBAL_USER, null, Set.of());
    var user = userRepository.findByUsername("baja").orElseThrow();
    users.update(
        user.id(),
        new InternalUserCommand(null, null, SecurityLevel.GLOBAL_USER, null, Set.of(), false));

    mvc.perform(get("/api/me").with(httpBasic("baja", PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicEndpointsStayOpen() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mvc.perform(post("/logout").with(csrf())).andExpect(status().is3xxRedirection());
  }
}
