package org.example.crm.common.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Marca: línea de producto que ejecuta programas CRM. La plataforma es multimarca y toda entidad de
 * negocio pertenece a una Marca.
 */
public final class Brand {

  private static final Pattern CODE = Pattern.compile("[a-z0-9][a-z0-9-]{1,31}");
  private static final Pattern DOMAIN =
      Pattern.compile(
          "(?=.{1,255}$)[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*");
  private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}(-[A-Z]{2})?");

  private final BrandId id;
  private final String code;
  private String name;
  private String domain;
  private String defaultLanguage;
  private BrandStatus status;

  private Brand(
      BrandId id,
      String code,
      String name,
      String domain,
      String defaultLanguage,
      BrandStatus status) {
    this.id = Objects.requireNonNull(id, "id");
    this.code = validateCode(code);
    this.name = validateName(name);
    this.domain = validateDomain(domain);
    this.defaultLanguage = validateLanguage(defaultLanguage);
    this.status = Objects.requireNonNull(status, "status");
  }

  /** Crea una Marca nueva y activa. */
  public static Brand create(String code, String name, String domain, String defaultLanguage) {
    return new Brand(BrandId.random(), code, name, domain, defaultLanguage, BrandStatus.ACTIVE);
  }

  /** Reconstruye una Marca existente (persistencia). */
  public static Brand restore(
      BrandId id,
      String code,
      String name,
      String domain,
      String defaultLanguage,
      BrandStatus status) {
    return new Brand(id, code, name, domain, defaultLanguage, status);
  }

  public void rename(String newName) {
    this.name = validateName(newName);
  }

  public void changeDomain(String newDomain) {
    this.domain = validateDomain(newDomain);
  }

  public void changeDefaultLanguage(String language) {
    this.defaultLanguage = validateLanguage(language);
  }

  public void activate() {
    this.status = BrandStatus.ACTIVE;
  }

  public void deactivate() {
    this.status = BrandStatus.INACTIVE;
  }

  public boolean isActive() {
    return status == BrandStatus.ACTIVE;
  }

  /** Indica si esta Marca sirve el host indicado (sin puerto, sin distinguir mayúsculas). */
  public boolean servesHost(String host) {
    if (domain == null || host == null) {
      return false;
    }
    String normalized = host.toLowerCase(Locale.ROOT);
    int colon = normalized.indexOf(':');
    if (colon >= 0) {
      normalized = normalized.substring(0, colon);
    }
    return domain.equals(normalized);
  }

  public BrandId id() {
    return id;
  }

  public String code() {
    return code;
  }

  public String name() {
    return name;
  }

  public Optional<String> domain() {
    return Optional.ofNullable(domain);
  }

  public String defaultLanguage() {
    return defaultLanguage;
  }

  public BrandStatus status() {
    return status;
  }

  private static String validateCode(String code) {
    if (code == null || !CODE.matcher(code).matches()) {
      throw new DomainException(
          "El código de Marca debe tener 2-32 caracteres [a-z0-9-] y empezar por letra o dígito: "
              + code);
    }
    return code;
  }

  private static String validateName(String name) {
    if (name == null || name.isBlank() || name.length() > 120) {
      throw new DomainException("El nombre de Marca es obligatorio (máx. 120 caracteres)");
    }
    return name.strip();
  }

  private static String validateDomain(String domain) {
    if (domain == null || domain.isBlank()) {
      return null;
    }
    String normalized = domain.strip().toLowerCase(Locale.ROOT);
    if (!DOMAIN.matcher(normalized).matches()) {
      throw new DomainException("Dominio de Marca no válido: " + domain);
    }
    return normalized;
  }

  private static String validateLanguage(String language) {
    if (language == null || !LANGUAGE.matcher(language).matches()) {
      throw new DomainException(
          "Idioma por defecto no válido (formato BCP-47, p. ej. 'es' o 'es-ES'): " + language);
    }
    return language;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Brand other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
