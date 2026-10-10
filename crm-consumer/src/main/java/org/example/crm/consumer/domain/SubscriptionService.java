package org.example.crm.consumer.domain;

import java.util.Objects;
import java.util.regex.Pattern;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;

/**
 * Servicio del programa al que un consumidor puede suscribirse (boletín, novedades, comunicaciones
 * periódicas…). Catálogo por Marca; el código es único en la Marca y es lo que usan las reglas
 * ({@code subscribedTo}).
 */
public final class SubscriptionService {

  private static final Pattern CODE = Pattern.compile("[a-z0-9][a-z0-9_-]{1,63}");

  private final SubscriptionServiceId id;
  private final BrandId brandId;
  private final String code;
  private String name;
  private String description;
  private boolean active;

  private SubscriptionService(
      SubscriptionServiceId id,
      BrandId brandId,
      String code,
      String name,
      String description,
      boolean active) {
    this.id = Objects.requireNonNull(id, "id");
    this.brandId = Objects.requireNonNull(brandId, "brandId");
    this.code = validateCode(code);
    this.name = validateName(name);
    this.description = validateDescription(description);
    this.active = active;
  }

  public static SubscriptionService create(
      BrandId brandId, String code, String name, String description) {
    return new SubscriptionService(
        SubscriptionServiceId.random(), brandId, code, name, description, true);
  }

  public static SubscriptionService restore(
      SubscriptionServiceId id,
      BrandId brandId,
      String code,
      String name,
      String description,
      boolean active) {
    return new SubscriptionService(id, brandId, code, name, description, active);
  }

  public void update(String newName, String newDescription, boolean nowActive) {
    this.name = validateName(newName);
    this.description = validateDescription(newDescription);
    this.active = nowActive;
  }

  public SubscriptionServiceId id() {
    return id;
  }

  public BrandId brandId() {
    return brandId;
  }

  public String code() {
    return code;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public boolean active() {
    return active;
  }

  private static String validateCode(String code) {
    if (code == null || !CODE.matcher(code).matches()) {
      throw new DomainException("Código de servicio no válido ([a-z0-9_-], 2-64): " + code);
    }
    return code;
  }

  private static String validateName(String name) {
    if (name == null || name.isBlank() || name.length() > 120) {
      throw new DomainException("El nombre del servicio es obligatorio (máx. 120)");
    }
    return name.strip();
  }

  private static String validateDescription(String description) {
    if (description != null && description.length() > 1000) {
      throw new DomainException("Descripción demasiado larga (máx. 1000)");
    }
    return description;
  }
}
