package org.example.crm.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.example.crm.admin.domain.FunctionalRole;
import org.example.crm.admin.domain.InternalUser;
import org.example.crm.admin.domain.SecurityLevel;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.domain.BrandId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class BrandAccessFilterTest {

  private final BrandId brandA = BrandId.random();
  private final BrandId brandB = BrandId.random();
  private final BrandAccessFilter filter = new BrandAccessFilter();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  private void authenticate(SecurityLevel level, BrandId brand) {
    InternalUserPrincipal principal =
        new InternalUserPrincipal(
            InternalUser.create("user1", "{noop}x", level, brand, Set.of(FunctionalRole.PRODUCER)));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  private BrandId run(MockHttpServletResponse response) throws Exception {
    AtomicReference<BrandId> seen = new AtomicReference<>();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/x"),
        response,
        new MockFilterChain(
            new jakarta.servlet.http.HttpServlet() {},
            (req, res, chain) -> seen.set(TenantContext.current().orElse(null))));
    return seen.get();
  }

  @Test
  void localUserDefaultsToOwnBrand() throws Exception {
    authenticate(SecurityLevel.LOCAL_USER, brandA);

    assertThat(run(new MockHttpServletResponse())).isEqualTo(brandA);
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void localUserCannotSelectAnotherBrand() throws Exception {
    authenticate(SecurityLevel.LOCAL_USER, brandA);
    TenantContext.set(brandB);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(run(response)).isNull();
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void globalUserCanSelectAnyBrand() throws Exception {
    authenticate(SecurityLevel.GLOBAL_USER, null);
    TenantContext.set(brandB);

    assertThat(run(new MockHttpServletResponse())).isEqualTo(brandB);
  }

  @Test
  void anonymousPassesThrough() throws Exception {
    assertThat(run(new MockHttpServletResponse())).isNull();
  }

  @Test
  void principalExposesLevelAndRoleAuthorities() {
    InternalUserPrincipal principal =
        new InternalUserPrincipal(
            InternalUser.create(
                "user1",
                "{noop}x",
                SecurityLevel.ADMIN,
                null,
                Set.of(FunctionalRole.DEVELOPER, FunctionalRole.CRM_MANAGER)));

    assertThat(principal.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("LEVEL_ADMIN", "ROLE_CRM_MANAGER", "ROLE_DEVELOPER");
    assertThat(principal.getUsername()).isEqualTo("user1");
    assertThat(principal.getPassword()).isEqualTo("{noop}x");
    assertThat(principal.isEnabled()).isTrue();
  }
}
