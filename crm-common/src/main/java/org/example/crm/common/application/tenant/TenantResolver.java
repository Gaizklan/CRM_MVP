package org.example.crm.common.application.tenant;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.example.crm.common.domain.NotFoundException;

/**
 * Resuelve la Marca de una petición.
 *
 * <ul>
 *   <li><b>Portal</b>: prefijo de ruta {@code /portal/{codigoMarca}/...} o, si no lo hay, dominio
 *       (cabecera Host) de la Marca. Solo Marcas activas.
 *   <li><b>Administración</b>: cabecera {@code X-Brand} o parámetro selector {@code brand}, con el
 *       código o el identificador de la Marca. Opcional: algunas operaciones de administración no
 *       dependen de Marca.
 * </ul>
 */
public class TenantResolver {

  public static final String PORTAL_PREFIX = "/portal/";

  private final BrandLookup brands;

  public TenantResolver(BrandLookup brands) {
    this.brands = brands;
  }

  /** Marca del portal según ruta o dominio; vacío si la petición no identifica ninguna. */
  public Optional<BrandId> resolvePortal(String host, String path) {
    Optional<String> codeFromPath = brandCodeFromPortalPath(path);
    Optional<Brand> brand =
        codeFromPath.isPresent()
            ? brands.findByCode(codeFromPath.get())
            : Optional.ofNullable(host)
                .map(TenantResolver::stripPort)
                .flatMap(brands::findByDomain);
    if (codeFromPath.isPresent() && brand.filter(Brand::isActive).isEmpty()) {
      throw new NotFoundException("Marca no encontrada: " + codeFromPath.get());
    }
    return brand.filter(Brand::isActive).map(Brand::id);
  }

  /**
   * Marca seleccionada en una petición de administración. La cabecera tiene prioridad sobre el
   * selector. Una Marca indicada pero inexistente es un error.
   */
  public Optional<BrandId> resolveAdmin(String headerValue, String selectorValue) {
    String selected = firstNonBlank(headerValue, selectorValue);
    if (selected == null) {
      return Optional.empty();
    }
    Optional<Brand> brand =
        looksLikeUuid(selected)
            ? brands.findById(BrandId.of(selected))
            : brands.findByCode(selected.toLowerCase(Locale.ROOT));
    return Optional.of(
        brand
            .map(Brand::id)
            .orElseThrow(() -> new DomainException("Marca desconocida: " + selected)));
  }

  static Optional<String> brandCodeFromPortalPath(String path) {
    if (path == null || !path.startsWith(PORTAL_PREFIX)) {
      return Optional.empty();
    }
    String rest = path.substring(PORTAL_PREFIX.length());
    int slash = rest.indexOf('/');
    String code = slash >= 0 ? rest.substring(0, slash) : rest;
    return code.isBlank() ? Optional.empty() : Optional.of(code.toLowerCase(Locale.ROOT));
  }

  private static String stripPort(String host) {
    int colon = host.indexOf(':');
    return (colon >= 0 ? host.substring(0, colon) : host).toLowerCase(Locale.ROOT);
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.strip();
    }
    if (b != null && !b.isBlank()) {
      return b.strip();
    }
    return null;
  }

  private static boolean looksLikeUuid(String value) {
    try {
      UUID.fromString(value);
      return value.length() == 36;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
