package org.example.crm.common.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.application.tenant.TenantResolver;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resuelve el {@link TenantContext} de cada petición antes de la seguridad y de los controladores.
 *
 * <ul>
 *   <li>{@code /api/**} (administración): cabecera {@value #BRAND_HEADER} o parámetro {@value
 *       #BRAND_PARAM}.
 *   <li>Resto (portal): prefijo {@code /portal/{marca}} o dominio de la Marca.
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

  public static final String BRAND_HEADER = "X-Brand";
  public static final String BRAND_PARAM = "brand";

  private final TenantResolver resolver;

  public TenantFilter(TenantResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<BrandId> brand;
    try {
      brand = resolve(request);
    } catch (NotFoundException e) {
      writeProblem(response, HttpStatus.NOT_FOUND, e.getMessage());
      return;
    } catch (DomainException e) {
      writeProblem(response, HttpStatus.BAD_REQUEST, e.getMessage());
      return;
    }
    try {
      brand.ifPresent(TenantContext::set);
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private Optional<BrandId> resolve(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (path.startsWith("/api/")) {
      return resolver.resolveAdmin(
          request.getHeader(BRAND_HEADER), request.getParameter(BRAND_PARAM));
    }
    return resolver.resolvePortal(request.getServerName(), path);
  }

  private static void writeProblem(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), escape(detail)));
  }

  private static String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
