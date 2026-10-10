package org.example.crm.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.example.crm.admin.application.InternalUserCommand;
import org.example.crm.admin.application.InternalUserRepository;
import org.example.crm.admin.application.InternalUserService;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.PageQuery;
import org.example.crm.common.application.tenant.BrandLookup;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.ConflictException;
import org.example.crm.consumer.application.ConsumerService;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerProfileChanged;
import org.example.crm.consumer.domain.ConsumerSearchCriteria;
import org.example.crm.consumer.domain.ConsumerStatus;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.example.crm.consumer.domain.PreferenceValue;
import org.example.crm.consumer.domain.ProfileUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ConsumerIT {

  static final String PASSWORD = "clave-de-prueba-123";

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Recoge los eventos ConsumerProfileChanged publicados. */
  @TestConfiguration
  static class Events {
    final List<ConsumerProfileChanged> received =
        java.util.Collections.synchronizedList(new ArrayList<>());

    @EventListener
    void on(ConsumerProfileChanged event) {
      received.add(event);
    }
  }

  @Autowired ConsumerService consumers;
  @Autowired BrandLookup brands;
  @Autowired InternalUserService users;
  @Autowired InternalUserRepository userRepository;
  @Autowired Events events;
  @Autowired MockMvc mvc;

  BrandId brandA;
  BrandId brandB;

  @BeforeEach
  void setUp() {
    brandA = brands.findByCode("marca-a").orElseThrow().id();
    brandB = brands.findByCode("marca-b").orElseThrow().id();
    if (userRepository.findByUsername("gestor.global").isEmpty()) {
      users.create(
          new InternalUserCommand(
              "gestor.global", PASSWORD, SecurityLevel.GLOBAL_USER, null, Set.of(), true));
      users.create(
          new InternalUserCommand(
              "gestor.local.b", PASSWORD, SecurityLevel.LOCAL_USER, brandB, Set.of(), true));
    }
  }

  private Consumer register(BrandId brand, String email) {
    return TenantContext.callAs(
        brand,
        () ->
            consumers.register(
                new Email(email), new PhoneNumber("+34600000001"), new LanguageTag("es")));
  }

  @Test
  void persistsProfilePreferencesAndHistory() {
    Consumer c = register(brandA, "persist@example.test");
    events.received.clear();

    TenantContext.runAs(
        brandA,
        () -> {
          consumers.updateProfile(
              c.id(),
              new ProfileUpdate(
                  null,
                  null,
                  new LanguageTag("en"),
                  Optional.of("S1"),
                  java.util.Map.of("city", "C1")));
          consumers.setPreference(
              c.id(), "budget", PreferenceValue.of(new BigDecimal("12.5")), "admin:test");
          consumers.setPreference(c.id(), "vip", PreferenceValue.of(true), "admin:test");
          consumers.setPreference(
              c.id(), "birthday", PreferenceValue.of(LocalDate.of(1990, 5, 1)), "admin:test");
          consumers.setPreference(
              c.id(), "budget", PreferenceValue.of(new BigDecimal("20")), "consumer");
        });

    Consumer loaded = TenantContext.callAs(brandA, () -> consumers.get(c.id()));
    assertThat(loaded.segment()).contains("S1");
    assertThat(loaded.language().value()).isEqualTo("en");
    assertThat(loaded.attributes()).containsEntry("city", "C1");
    assertThat(loaded.preference("budget")).contains(PreferenceValue.of(new BigDecimal("20")));
    assertThat(loaded.preference("vip")).contains(PreferenceValue.of(true));
    assertThat(loaded.preference("birthday"))
        .contains(PreferenceValue.of(LocalDate.of(1990, 5, 1)));
    var history = TenantContext.callAs(brandA, () -> consumers.preferenceHistory(c.id()));
    assertThat(history).hasSize(4);
    assertThat(history)
        .anySatisfy(
            h -> {
              assertThat(h.key()).isEqualTo("budget");
              assertThat(h.previous()).contains(PreferenceValue.of(new BigDecimal("12.5")));
              assertThat(h.current()).contains(PreferenceValue.of(new BigDecimal("20")));
            });
    assertThat(events.received).hasSize(5);
    assertThat(events.received.get(0).changedFields())
        .containsExactlyInAnyOrder("language", "segment", "attribute.city");
  }

  @Test
  void emailIsUniquePerBrandInDatabase() {
    register(brandA, "unico@example.test");

    assertThatThrownBy(() -> register(brandA, "unico@example.test"))
        .isInstanceOf(ConflictException.class);
    assertThat(register(brandB, "unico@example.test").brandId()).isEqualTo(brandB);
  }

  @Test
  void searchFiltersAndPaginates() {
    for (int i = 0; i < 5; i++) {
      Consumer c = register(brandA, "pag" + i + "@example.test");
      if (i % 2 == 0) {
        TenantContext.runAs(
            brandA,
            () ->
                consumers.updateProfile(
                    c.id(), new ProfileUpdate(null, null, null, Optional.of("SPAG"), null)));
      }
    }
    TenantContext.runAs(
        brandA,
        () -> {
          var page =
              consumers.search(
                  new ConsumerSearchCriteria(null, ConsumerStatus.ACTIVE, null, "SPAG", null, null),
                  new PageQuery(0, 2));
          assertThat(page.totalElements()).isEqualTo(3);
          assertThat(page.content()).hasSize(2);
          assertThat(page.totalPages()).isEqualTo(2);
          assertThat(
                  consumers
                      .search(
                          new ConsumerSearchCriteria(
                              new Email("pag1@example.test"), null, null, null, null, null),
                          new PageQuery(0, 10))
                      .content())
              .extracting(x -> x.email().value())
              .containsExactly("pag1@example.test");
        });
  }

  @Test
  void adminApiIsPaginatedFilterableAndBrandScoped() throws Exception {
    Consumer a = register(brandA, "api.a@example.test");
    Consumer b = register(brandB, "api.b@example.test");

    mvc.perform(
            get("/api/consumers")
                .header("X-Brand", "marca-a")
                .param("email", "api.a@example.test")
                .param("size", "5")
                .with(httpBasic("gestor.global", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.content[0].id").value(a.id().toString()));
    mvc.perform(
            get("/api/consumers/" + a.id())
                .header("X-Brand", "marca-b")
                .with(httpBasic("gestor.global", PASSWORD)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/consumers/" + b.id()).with(httpBasic("gestor.local.b", PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("api.b@example.test"));
    mvc.perform(get("/api/consumers").with(httpBasic("gestor.global", PASSWORD)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/consumers")
                .header("X-Brand", "marca-a")
                .param("size", "500")
                .with(httpBasic("gestor.global", PASSWORD)))
        .andExpect(status().isBadRequest());
  }
}
