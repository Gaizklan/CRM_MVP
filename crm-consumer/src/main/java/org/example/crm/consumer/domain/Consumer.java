package org.example.crm.consumer.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;
import org.example.crm.common.domain.DomainException;

/**
 * Consumidor de una Marca: datos personales, idioma, segmento, atributos de perfil y preferencias
 * tipadas. Cada cambio de perfil o preferencias registra un evento {@link ConsumerProfileChanged}
 * y, para las preferencias, una entrada de historial ({@link PreferenceChange}).
 */
public final class Consumer {

  private static final Pattern KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");
  private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9_-]{1,32}");

  private final ConsumerId id;
  private final BrandId brandId;
  private final Instant registeredAt;
  private Email email;
  private PhoneNumber phone;
  private LanguageTag language;
  private String segment;
  private ConsumerStatus status;
  private final Map<String, String> attributes;
  private final Map<String, PreferenceValue> preferences;

  private final List<DomainEvent> pendingEvents = new ArrayList<>();
  private final List<PreferenceChange> pendingPreferenceChanges = new ArrayList<>();

  private Consumer(
      ConsumerId id,
      BrandId brandId,
      Email email,
      PhoneNumber phone,
      LanguageTag language,
      String segment,
      ConsumerStatus status,
      Instant registeredAt,
      Map<String, String> attributes,
      Map<String, PreferenceValue> preferences) {
    this.id = Objects.requireNonNull(id, "id");
    this.brandId = Objects.requireNonNull(brandId, "brandId");
    this.email = Objects.requireNonNull(email, "email");
    this.phone = phone;
    this.language = Objects.requireNonNull(language, "language");
    this.segment = validateSegment(segment);
    this.status = Objects.requireNonNull(status, "status");
    this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt");
    this.attributes = new TreeMap<>();
    attributes.forEach((k, v) -> this.attributes.put(validateKey(k), validateAttribute(v)));
    this.preferences = new TreeMap<>();
    preferences.forEach((k, v) -> this.preferences.put(validateKey(k), Objects.requireNonNull(v)));
  }

  /** Alta de un consumidor activo. */
  public static Consumer register(
      BrandId brandId, Email email, PhoneNumber phone, LanguageTag language, Clock clock) {
    return new Consumer(
        ConsumerId.random(),
        brandId,
        email,
        phone,
        language,
        null,
        ConsumerStatus.ACTIVE,
        clock.instant(),
        Map.of(),
        Map.of());
  }

  /** Reconstrucción desde persistencia (no genera eventos). */
  public static Consumer restore(
      ConsumerId id,
      BrandId brandId,
      Email email,
      PhoneNumber phone,
      LanguageTag language,
      String segment,
      ConsumerStatus status,
      Instant registeredAt,
      Map<String, String> attributes,
      Map<String, PreferenceValue> preferences) {
    return new Consumer(
        id,
        brandId,
        email,
        phone,
        language,
        segment,
        status,
        registeredAt,
        attributes,
        preferences);
  }

  // ---- Perfil ----

  /**
   * Aplica un cambio de perfil. Solo los campos presentes en {@code update} se modifican; los
   * atributos con valor {@code null} se eliminan.
   */
  public void updateProfile(ProfileUpdate update, Clock clock) {
    requireNotWithdrawn();
    Set<String> changed = new LinkedHashSet<>();
    if (update.email() != null && !update.email().equals(email)) {
      email = update.email();
      changed.add("email");
    }
    if (update.phone() != null && !update.phone().equals(Optional.ofNullable(phone))) {
      phone = update.phone().orElse(null);
      changed.add("phone");
    }
    if (update.language() != null && !update.language().equals(language)) {
      language = update.language();
      changed.add("language");
    }
    if (update.segment() != null) {
      String newSegment = validateSegment(update.segment().orElse(null));
      if (!Objects.equals(newSegment, segment)) {
        segment = newSegment;
        changed.add("segment");
      }
    }
    if (update.attributes() != null) {
      for (Map.Entry<String, String> e : update.attributes().entrySet()) {
        String key = validateKey(e.getKey());
        String value = e.getValue() == null ? null : validateAttribute(e.getValue());
        String previous = value == null ? attributes.remove(key) : attributes.put(key, value);
        if (!Objects.equals(previous, value)) {
          changed.add("attribute." + key);
        }
      }
    }
    recordChange(changed, clock);
  }

  // ---- Preferencias ----

  /** Crea o modifica una preferencia; registra historial y evento si el valor cambia. */
  public void setPreference(String key, PreferenceValue value, String changedBy, Clock clock) {
    requireNotWithdrawn();
    String k = validateKey(key);
    Objects.requireNonNull(value, "value");
    PreferenceValue previous = preferences.get(k);
    if (value.equals(previous)) {
      return;
    }
    preferences.put(k, value);
    trackPreference(k, previous, value, changedBy, clock);
  }

  /** Elimina una preferencia si existe. */
  public void removePreference(String key, String changedBy, Clock clock) {
    requireNotWithdrawn();
    String k = validateKey(key);
    PreferenceValue previous = preferences.remove(k);
    if (previous != null) {
      trackPreference(k, previous, null, changedBy, clock);
    }
  }

  private void trackPreference(
      String key, PreferenceValue previous, PreferenceValue value, String changedBy, Clock clock) {
    Instant now = clock.instant();
    pendingPreferenceChanges.add(
        new PreferenceChange(brandId, id, key, previous, value, requireActor(changedBy), now));
    recordChange(Set.of("preference." + key), clock);
  }

  // ---- Estado ----

  public void lock(Clock clock) {
    transition(ConsumerStatus.LOCKED, clock);
  }

  public void unlock(Clock clock) {
    if (status == ConsumerStatus.LOCKED) {
      transition(ConsumerStatus.ACTIVE, clock);
    }
  }

  /** Baja del programa. Estado final: no admite más cambios. */
  public void withdraw(Clock clock) {
    transition(ConsumerStatus.WITHDRAWN, clock);
  }

  private void transition(ConsumerStatus target, Clock clock) {
    requireNotWithdrawn();
    if (status != target) {
      status = target;
      recordChange(Set.of("status"), clock);
    }
  }

  // ---- Eventos pendientes ----

  /** Devuelve y vacía los eventos de dominio pendientes de publicar. */
  public List<DomainEvent> pullEvents() {
    List<DomainEvent> events = List.copyOf(pendingEvents);
    pendingEvents.clear();
    return events;
  }

  /** Devuelve y vacía las entradas de historial de preferencias pendientes de persistir. */
  public List<PreferenceChange> pullPreferenceChanges() {
    List<PreferenceChange> changes = List.copyOf(pendingPreferenceChanges);
    pendingPreferenceChanges.clear();
    return changes;
  }

  private void recordChange(Set<String> changed, Clock clock) {
    if (changed.isEmpty()) {
      return;
    }
    // Un único evento por operación; si ya hay uno pendiente, se fusionan los campos.
    Set<String> fields = new LinkedHashSet<>(changed);
    pendingEvents.removeIf(
        e -> {
          if (e instanceof ConsumerProfileChanged p) {
            fields.addAll(p.changedFields());
            return true;
          }
          return false;
        });
    pendingEvents.add(new ConsumerProfileChanged(brandId, id, fields, clock.instant()));
  }

  private void requireNotWithdrawn() {
    if (status == ConsumerStatus.WITHDRAWN) {
      throw new DomainException("El consumidor está dado de baja y no admite cambios");
    }
  }

  private static String validateKey(String key) {
    if (key == null || !KEY.matcher(key).matches()) {
      throw new DomainException(
          "Clave no válida (letra inicial, [a-zA-Z0-9_], máx. 64 caracteres): " + key);
    }
    return key;
  }

  private static String validateAttribute(String value) {
    if (value.length() > 1000) {
      throw new DomainException("Valor de atributo demasiado largo (máx. 1000 caracteres)");
    }
    return value;
  }

  private static String validateSegment(String segment) {
    if (segment == null) {
      return null;
    }
    if (!SEGMENT.matcher(segment).matches()) {
      throw new DomainException("Segmento no válido ([A-Za-z0-9_-], máx. 32): " + segment);
    }
    return segment;
  }

  private static String requireActor(String actor) {
    if (actor == null || actor.isBlank()) {
      throw new DomainException("Se requiere el autor del cambio");
    }
    return actor;
  }

  // ---- Lectura ----

  public ConsumerId id() {
    return id;
  }

  public BrandId brandId() {
    return brandId;
  }

  public Email email() {
    return email;
  }

  public Optional<PhoneNumber> phone() {
    return Optional.ofNullable(phone);
  }

  public LanguageTag language() {
    return language;
  }

  public Optional<String> segment() {
    return Optional.ofNullable(segment);
  }

  public ConsumerStatus status() {
    return status;
  }

  public Instant registeredAt() {
    return registeredAt;
  }

  public Map<String, String> attributes() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
  }

  public Map<String, PreferenceValue> preferences() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(preferences));
  }

  public Optional<PreferenceValue> preference(String key) {
    return Optional.ofNullable(preferences.get(key));
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Consumer other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
