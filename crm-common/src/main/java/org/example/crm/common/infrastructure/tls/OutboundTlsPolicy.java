package org.example.crm.common.infrastructure.tls;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Política de TLS obligatoria con proveedores externos (SMTP con STARTTLS, pasarelas por HTTPS).
 * Los adaptadores de canal (C03, C04, C21) deben validar sus endpoints con esta política.
 *
 * <p>Solo si {@code allowInsecureLocalhost} está activo (perfil de desarrollo) se permiten
 * conexiones sin TLS a {@code localhost}, para MailHog y el mock SMS de docker compose.
 */
public class OutboundTlsPolicy {

  private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  private final boolean allowInsecureLocalhost;

  public OutboundTlsPolicy(boolean allowInsecureLocalhost) {
    this.allowInsecureLocalhost = allowInsecureLocalhost;
  }

  /** Valida el endpoint HTTP de un proveedor: debe ser {@code https}. */
  public URI requireHttps(URI endpoint) {
    String scheme =
        endpoint.getScheme() == null ? "" : endpoint.getScheme().toLowerCase(Locale.ROOT);
    if (scheme.equals("https")) {
      return endpoint;
    }
    if (scheme.equals("http") && insecureAllowedFor(endpoint.getHost())) {
      return endpoint;
    }
    throw new IllegalStateException(
        "TLS obligatorio: el proveedor externo debe usar https (" + endpoint + ")");
  }

  /** Propiedades JavaMail para un servidor SMTP: STARTTLS obligatorio y verificación del host. */
  public Map<String, String> smtpProperties(String host) {
    if (insecureAllowedFor(host)) {
      return Map.of("mail.smtp.starttls.enable", "false");
    }
    return Map.of(
        "mail.smtp.starttls.enable", "true",
        "mail.smtp.starttls.required", "true",
        "mail.smtp.ssl.checkserveridentity", "true");
  }

  private boolean insecureAllowedFor(String host) {
    return allowInsecureLocalhost
        && host != null
        && LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT));
  }
}
