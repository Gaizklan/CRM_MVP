package org.example.crm.common.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.application.tenant.TenantResolver;
import org.example.crm.common.application.tenant.TenantResolverTest;
import org.example.crm.common.domain.BrandId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantFilterTest {

  private final TenantFilter filter =
      new TenantFilter(new TenantResolver(new TenantResolverTest.InMemoryBrands()));

  @Test
  void adminRequestUsesBrandHeaderAndClearsAfterwards() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/consumers");
    request.addHeader(TenantFilter.BRAND_HEADER, "marca-b");
    AtomicReference<BrandId> seen = new AtomicReference<>();

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        new MockFilterChain(
            new jakarta.servlet.http.HttpServlet() {},
            (req, res, chain) -> seen.set(TenantContext.require())));

    assertThat(seen.get()).isEqualTo(TenantResolverTest.B.id());
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void portalRequestUsesDomain() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
    request.setServerName("marca-a.example.test");
    AtomicReference<BrandId> seen = new AtomicReference<>();

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        new MockFilterChain(
            new jakarta.servlet.http.HttpServlet() {},
            (req, res, chain) -> seen.set(TenantContext.current().orElse(null))));

    assertThat(seen.get()).isEqualTo(TenantResolverTest.A.id());
  }

  @Test
  void unknownAdminBrandIsBadRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/consumers");
    request.setParameter(TenantFilter.BRAND_PARAM, "marca-z");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("marca-z");
  }

  @Test
  void unknownPortalPrefixIsNotFound() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/nope/home");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(404);
  }
}
