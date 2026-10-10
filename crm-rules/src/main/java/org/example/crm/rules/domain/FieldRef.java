package org.example.crm.rules.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.example.crm.consumer.domain.ConsentChannel;

/**
 * Campo referenciado por una regla.
 *
 * <ul>
 *   <li>Perfil: {@code email}, {@code phone}, {@code language}, {@code segment}, {@code status}
 *       (texto) y {@code registeredAt} (fecha/hora).
 *   <li>{@code attribute.<nombre>}: atributo de perfil (texto).
 *   <li>{@code preference.<clave>}: preferencia tipada (texto, número, booleano o fecha).
 *   <li>{@code consent.<CANAL>}: {@code true} si el canal está permitido (opt-in del canal y del
 *       programa).
 *   <li>{@code subscriptions}: códigos de los servicios suscritos.
 * </ul>
 */
public record FieldRef(String path, Kind kind, String name, ValueType type) {

  /** Categoría del campo. */
  public enum Kind {
    PROFILE,
    ATTRIBUTE,
    PREFERENCE,
    CONSENT,
    SUBSCRIPTIONS
  }

  public static final Map<String, ValueType> PROFILE_FIELDS =
      Map.of(
          "email", ValueType.STRING,
          "phone", ValueType.STRING,
          "language", ValueType.STRING,
          "segment", ValueType.STRING,
          "status", ValueType.STRING,
          "registeredAt", ValueType.DATETIME);

  private static final Pattern KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");

  public FieldRef {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(type, "type");
  }

  /** Interpreta la ruta de un campo; vacío si no es válida. */
  public static Optional<FieldRef> parse(String path) {
    if (path == null) {
      return Optional.empty();
    }
    if (PROFILE_FIELDS.containsKey(path)) {
      return Optional.of(new FieldRef(path, Kind.PROFILE, path, PROFILE_FIELDS.get(path)));
    }
    if (path.equals("subscriptions")) {
      return Optional.of(new FieldRef(path, Kind.SUBSCRIPTIONS, path, ValueType.SET));
    }
    int dot = path.indexOf('.');
    if (dot < 0) {
      return Optional.empty();
    }
    String prefix = path.substring(0, dot);
    String key = path.substring(dot + 1);
    return switch (prefix) {
      case "attribute" ->
          KEY.matcher(key).matches()
              ? Optional.of(new FieldRef(path, Kind.ATTRIBUTE, key, ValueType.STRING))
              : Optional.empty();
      case "preference" ->
          KEY.matcher(key).matches()
              ? Optional.of(new FieldRef(path, Kind.PREFERENCE, key, ValueType.DYNAMIC))
              : Optional.empty();
      case "consent" ->
          Arrays.stream(ConsentChannel.values()).anyMatch(c -> c.name().equals(key))
              ? Optional.of(new FieldRef(path, Kind.CONSENT, key, ValueType.BOOLEAN))
              : Optional.empty();
      default -> Optional.empty();
    };
  }

  /** Descripción de los campos válidos, para mensajes de error. */
  public static String describeValidFields() {
    return "email, phone, language, segment, status, registeredAt, attribute.<nombre>,"
        + " preference.<clave>, consent.<"
        + String.join("|", Arrays.stream(ConsentChannel.values()).map(Enum::name).toList())
        + ">, subscriptions";
  }
}
