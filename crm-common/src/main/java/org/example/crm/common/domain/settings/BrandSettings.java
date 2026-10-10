package org.example.crm.common.domain.settings;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.DomainException;

/**
 * Parámetros de negocio de una Marca, modificables sin redespliegue (NFR-4).
 *
 * @param notifications remitente y asunto por tipo de notificación (todos los tipos)
 * @param languages idiomas habilitados; el primero es el idioma por defecto del programa
 * @param maxRetries número de reintentos ante error del proveedor, por canal (0–10)
 * @param loginLockThreshold intentos de login fallidos antes de bloquear (1–20)
 * @param activeProviders códigos de proveedores activos por canal, en orden de preferencia
 */
public record BrandSettings(
    Map<NotificationType, NotificationSettings> notifications,
    List<String> languages,
    Map<DeliveryChannel, Integer> maxRetries,
    int loginLockThreshold,
    Map<DeliveryChannel, List<String>> activeProviders) {

  public static final int MAX_RETRIES = 10;
  public static final int MAX_LOGIN_LOCK_THRESHOLD = 20;
  public static final int MAX_PROVIDERS_PER_CHANNEL = 10;

  private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}(-[A-Z]{2})?");
  private static final Pattern PROVIDER = Pattern.compile("[a-z0-9][a-z0-9_-]{1,63}");

  public BrandSettings {
    notifications = complete(notifications, NotificationType.class, "notifications");
    for (var e : notifications.entrySet()) {
      if (e.getValue() == null) {
        throw new DomainException("Falta la configuración de la notificación " + e.getKey());
      }
    }
    if (languages == null || languages.isEmpty() || languages.size() > 20) {
      throw new DomainException("Se requiere al menos un idioma (máx. 20)");
    }
    for (String language : languages) {
      if (language == null || !LANGUAGE.matcher(language).matches()) {
        throw new DomainException("Idioma no válido (BCP-47, p. ej. 'es' o 'es-ES'): " + language);
      }
    }
    if (new LinkedHashSet<>(languages).size() != languages.size()) {
      throw new DomainException("Hay idiomas repetidos");
    }
    languages = List.copyOf(languages);
    maxRetries = complete(maxRetries, DeliveryChannel.class, "maxRetries");
    for (var e : maxRetries.entrySet()) {
      if (e.getValue() == null || e.getValue() < 0 || e.getValue() > MAX_RETRIES) {
        throw new DomainException(
            "Reintentos de " + e.getKey() + " fuera de rango (0-" + MAX_RETRIES + ")");
      }
    }
    if (loginLockThreshold < 1 || loginLockThreshold > MAX_LOGIN_LOCK_THRESHOLD) {
      throw new DomainException(
          "El umbral de bloqueo de login debe estar entre 1 y " + MAX_LOGIN_LOCK_THRESHOLD);
    }
    activeProviders = complete(activeProviders, DeliveryChannel.class, "activeProviders");
    EnumMap<DeliveryChannel, List<String>> providers = new EnumMap<>(DeliveryChannel.class);
    for (var e : activeProviders.entrySet()) {
      List<String> list = e.getValue() == null ? List.of() : e.getValue();
      if (list.size() > MAX_PROVIDERS_PER_CHANNEL
          || new LinkedHashSet<>(list).size() != list.size()
          || list.stream().anyMatch(p -> p == null || !PROVIDER.matcher(p).matches())) {
        throw new DomainException(
            "Proveedores de "
                + e.getKey()
                + " no válidos (códigos [a-z0-9_-] únicos, máx. "
                + MAX_PROVIDERS_PER_CHANNEL
                + ")");
      }
      providers.put(e.getKey(), List.copyOf(list));
    }
    activeProviders = Collections.unmodifiableMap(providers);
  }

  /** Idioma por defecto del programa (el primero de {@link #languages}). */
  public String defaultLanguage() {
    return languages.get(0);
  }

  public NotificationSettings notification(NotificationType type) {
    return notifications.get(type);
  }

  public int maxRetries(DeliveryChannel channel) {
    return maxRetries.get(channel);
  }

  public List<String> providers(DeliveryChannel channel) {
    return activeProviders.get(channel);
  }

  /**
   * Valores por defecto de una Marca nueva (documentados en {@code docs/brand-settings.md}).
   *
   * <ul>
   *   <li>Remitente: nombre de la Marca, {@code no-reply@<dominio de la Marca>} (o {@code
   *       no-reply@example.invalid} si no tiene dominio); asuntos genéricos en español.
   *   <li>Idiomas: el idioma por defecto de la Marca.
   *   <li>Reintentos: EMAIL 3, SMS 3, MMS 3, PUSH 2, INBOX 0.
   *   <li>Bloqueo de login: 5 intentos fallidos.
   *   <li>Proveedores activos: ninguno (se configuran al dar de alta los canales).
   * </ul>
   */
  public static BrandSettings defaults(Brand brand) {
    String address = "no-reply@" + brand.domain().orElse("example.invalid");
    EnumMap<NotificationType, NotificationSettings> notifications =
        new EnumMap<>(NotificationType.class);
    notifications.put(
        NotificationType.REGISTRATION_VERIFICATION,
        new NotificationSettings(brand.name(), address, "Confirma tu registro"));
    notifications.put(
        NotificationType.PASSWORD_RESET,
        new NotificationSettings(brand.name(), address, "Restablece tu contraseña"));
    notifications.put(
        NotificationType.ACCOUNT_LOCKED,
        new NotificationSettings(brand.name(), address, "Tu cuenta se ha bloqueado"));
    notifications.put(
        NotificationType.UNSUBSCRIBE_CONFIRMATION,
        new NotificationSettings(brand.name(), address, "Confirmación de baja"));
    EnumMap<DeliveryChannel, Integer> retries = new EnumMap<>(DeliveryChannel.class);
    retries.put(DeliveryChannel.EMAIL, 3);
    retries.put(DeliveryChannel.SMS, 3);
    retries.put(DeliveryChannel.MMS, 3);
    retries.put(DeliveryChannel.PUSH, 2);
    retries.put(DeliveryChannel.INBOX, 0);
    EnumMap<DeliveryChannel, List<String>> providers = new EnumMap<>(DeliveryChannel.class);
    for (DeliveryChannel channel : DeliveryChannel.values()) {
      providers.put(channel, List.of());
    }
    return new BrandSettings(
        notifications, List.of(brand.defaultLanguage()), retries, 5, providers);
  }

  private static <K extends Enum<K>, V> Map<K, V> complete(
      Map<K, V> map, Class<K> keys, String name) {
    if (map == null) {
      throw new DomainException("Falta '" + name + "'");
    }
    EnumMap<K, V> copy = new EnumMap<>(keys);
    copy.putAll(map);
    for (K key : keys.getEnumConstants()) {
      if (!copy.containsKey(key)) {
        throw new DomainException("Falta '" + name + "." + key.name() + "'");
      }
    }
    return Collections.unmodifiableMap(copy);
  }
}
