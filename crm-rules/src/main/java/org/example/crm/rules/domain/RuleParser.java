package org.example.crm.rules.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.example.crm.rules.domain.Rule.Op;

/**
 * Convierte el árbol JSON genérico de una regla ({@code Map}/{@code List}/escalares, tal como lo
 * produce cualquier parser JSON) en un {@link Rule} validado. Recoge todos los errores con su ruta
 * (p. ej. {@code $.and[1].gt.value}) y lanza {@link RuleValidationException}.
 *
 * <p>Formato: cada nodo es un objeto con una única clave, el operador:
 *
 * <pre>
 * {"and": [regla, ...]}            {"or": [regla, ...]}            {"not": regla}
 * {"eq"|"ne"|"gt"|"lt": {"field": campo, "value": literal}}
 * {"in": {"field": campo, "values": [literal, ...]}}
 * {"between": {"field": campo, "from": literal, "to": literal}}
 * {"exists": {"field": campo}}
 * {"contains": {"field": campo, "value": literal}}
 * {"subscribedTo": {"service": "codigo"}}
 * {"eventOccurred": {"type": "TIPO", "within": "P2D"}}
 * </pre>
 */
public final class RuleParser {

  public static final int MAX_DEPTH = 20;
  public static final int MAX_NODES = 500;
  public static final int MAX_IN_VALUES = 1000;
  public static final Duration MAX_WITHIN = Duration.ofDays(366);

  static final Set<String> OPERATORS =
      Set.of(
          "and",
          "or",
          "not",
          "eq",
          "ne",
          "in",
          "gt",
          "lt",
          "between",
          "exists",
          "contains",
          "subscribedTo",
          "eventOccurred");

  private static final Pattern SERVICE_CODE = Pattern.compile("[a-z0-9][a-z0-9_-]{1,63}");
  private static final Pattern EVENT_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

  private final List<String> errors = new ArrayList<>();
  private int nodes;

  private RuleParser() {}

  /** Parsea y valida; lanza {@link RuleValidationException} con todos los errores encontrados. */
  public static Rule parse(Object tree) {
    RuleParser parser = new RuleParser();
    Rule rule = parser.node(tree, "$", 1);
    if (!parser.errors.isEmpty()) {
      throw new RuleValidationException(parser.errors);
    }
    return rule;
  }

  /** Lista de errores de validación (vacía si la regla es válida). */
  public static List<String> validate(Object tree) {
    try {
      parse(tree);
      return List.of();
    } catch (RuleValidationException e) {
      return e.errors();
    }
  }

  private Rule node(Object tree, String path, int depth) {
    if (depth > MAX_DEPTH) {
      return error(path, "anidamiento máximo superado (" + MAX_DEPTH + " niveles)");
    }
    if (++nodes > MAX_NODES) {
      return error(path, "la regla supera el máximo de " + MAX_NODES + " nodos");
    }
    if (!(tree instanceof Map<?, ?> map)) {
      return error(path, "se esperaba un objeto con un operador (" + operators() + ")");
    }
    if (map.size() != 1) {
      return error(
          path, "cada nodo debe tener exactamente un operador, encontrado(s): " + map.keySet());
    }
    Map.Entry<?, ?> entry = map.entrySet().iterator().next();
    String op = String.valueOf(entry.getKey());
    Object arg = entry.getValue();
    String p = path + "." + op;
    return switch (op) {
      case "and", "or" -> logical(op, arg, p, depth);
      case "not" -> {
        Rule inner = node(arg, p, depth + 1);
        yield inner == null ? null : new Rule.Not(inner);
      }
      case "eq" -> compare(Op.EQ, arg, p);
      case "ne" -> compare(Op.NE, arg, p);
      case "gt" -> compare(Op.GT, arg, p);
      case "lt" -> compare(Op.LT, arg, p);
      case "in" -> in(arg, p);
      case "between" -> between(arg, p);
      case "exists" -> exists(arg, p);
      case "contains" -> contains(arg, p);
      case "subscribedTo" -> subscribedTo(arg, p);
      case "eventOccurred" -> eventOccurred(arg, p);
      default -> error(path, "operador desconocido '" + op + "'; válidos: " + operators());
    };
  }

  private Rule logical(String op, Object arg, String path, int depth) {
    if (!(arg instanceof List<?> list) || list.isEmpty()) {
      return error(path, "se esperaba una lista no vacía de reglas");
    }
    List<Rule> children = new ArrayList<>();
    for (int i = 0; i < list.size(); i++) {
      children.add(node(list.get(i), path + "[" + i + "]", depth + 1));
    }
    if (children.contains(null)) {
      return null;
    }
    return op.equals("and") ? new Rule.And(children) : new Rule.Or(children);
  }

  private Rule compare(Op op, Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("field", "value"));
    if (args == null) {
      return null;
    }
    FieldRef field = field(args.get("field"), path + ".field");
    Literal value = literal(args.get("value"), path + ".value");
    if (field == null || value == null) {
      return null;
    }
    boolean ordering = op == Op.GT || op == Op.LT;
    if (ordering
        ? !checkOrdered(field, value, path + ".value")
        : !checkEquality(field, value, path + ".value")) {
      return null;
    }
    return new Rule.Compare(op, field, value);
  }

  private Rule in(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("field", "values"));
    if (args == null) {
      return null;
    }
    FieldRef field = field(args.get("field"), path + ".field");
    Object raw = args.get("values");
    if (!(raw instanceof List<?> list) || list.isEmpty() || list.size() > MAX_IN_VALUES) {
      error(path + ".values", "se esperaba una lista de 1 a " + MAX_IN_VALUES + " literales");
      return null;
    }
    List<Literal> values = new ArrayList<>();
    for (int i = 0; i < list.size(); i++) {
      Literal value = literal(list.get(i), path + ".values[" + i + "]");
      if (value != null
          && field != null
          && checkEquality(field, value, path + ".values[" + i + "]")) {
        values.add(value);
      }
    }
    if (field == null || values.size() != list.size()) {
      return null;
    }
    return new Rule.In(field, values);
  }

  private Rule between(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("field", "from", "to"));
    if (args == null) {
      return null;
    }
    FieldRef field = field(args.get("field"), path + ".field");
    Literal from = literal(args.get("from"), path + ".from");
    Literal to = literal(args.get("to"), path + ".to");
    if (field == null || from == null || to == null) {
      return null;
    }
    boolean ok = checkOrdered(field, from, path + ".from") & checkOrdered(field, to, path + ".to");
    if (!ok) {
      return null;
    }
    if (from.getClass() != to.getClass()) {
      return error(path, "'from' y 'to' deben ser del mismo tipo");
    }
    return new Rule.Between(field, from, to);
  }

  private Rule exists(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("field"));
    if (args == null) {
      return null;
    }
    FieldRef field = field(args.get("field"), path + ".field");
    return field == null ? null : new Rule.Exists(field);
  }

  private Rule contains(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("field", "value"));
    if (args == null) {
      return null;
    }
    FieldRef field = field(args.get("field"), path + ".field");
    Literal value = literal(args.get("value"), path + ".value");
    if (field == null || value == null) {
      return null;
    }
    if (field.type() == ValueType.BOOLEAN || field.type() == ValueType.DATETIME) {
      return error(
          path + ".field", "'contains' solo se aplica a textos, preferencias o subscriptions");
    }
    if (!(value instanceof Literal.Text text) || text.value().isEmpty()) {
      return error(path + ".value", "'contains' requiere un texto no vacío");
    }
    return new Rule.Contains(field, value);
  }

  private Rule subscribedTo(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("service"));
    if (args == null) {
      return null;
    }
    Object service = args.get("service");
    if (!(service instanceof String code) || !SERVICE_CODE.matcher(code).matches()) {
      return error(path + ".service", "se esperaba el código de un servicio ([a-z0-9_-], 2-64)");
    }
    return new Rule.SubscribedTo(code);
  }

  private Rule eventOccurred(Object arg, String path) {
    Map<?, ?> args = object(arg, path, Set.of("type", "within"));
    if (args == null) {
      return null;
    }
    Object type = args.get("type");
    Object within = args.get("within");
    String typeName = null;
    if (!(type instanceof String t) || !EVENT_TYPE.matcher(t).matches()) {
      error(path + ".type", "se esperaba un tipo de evento en mayúsculas (p. ej. EMAIL_OPENED)");
    } else {
      typeName = t;
    }
    Duration duration = null;
    try {
      duration = within instanceof String w ? Duration.parse(w) : null;
    } catch (DateTimeParseException e) {
      duration = null;
    }
    if (duration == null
        || duration.isNegative()
        || duration.isZero()
        || duration.compareTo(MAX_WITHIN) > 0) {
      error(
          path + ".within",
          "se esperaba una duración ISO-8601 positiva de hasta 366 días (p. ej. P2D, PT12H)");
      return null;
    }
    return typeName == null ? null : new Rule.EventOccurred(typeName, duration);
  }

  // ---- utilidades ----

  private Map<?, ?> object(Object arg, String path, Set<String> keys) {
    if (!(arg instanceof Map<?, ?> map)) {
      error(path, "se esperaba un objeto con " + keys);
      return null;
    }
    List<String> missing = keys.stream().filter(k -> !map.containsKey(k)).sorted().toList();
    List<String> unknown =
        map.keySet().stream().map(String::valueOf).filter(k -> !keys.contains(k)).sorted().toList();
    if (!missing.isEmpty()) {
      error(path, "faltan las propiedades " + missing);
    }
    if (!unknown.isEmpty()) {
      error(path, "propiedades no reconocidas " + unknown);
    }
    return missing.isEmpty() && unknown.isEmpty() ? map : null;
  }

  private FieldRef field(Object raw, String path) {
    Optional<FieldRef> field = raw instanceof String s ? FieldRef.parse(s) : Optional.empty();
    if (field.isEmpty()) {
      error(path, "campo desconocido '" + raw + "'; válidos: " + FieldRef.describeValidFields());
      return null;
    }
    return field.get();
  }

  private Literal literal(Object raw, String path) {
    return switch (raw) {
      case null -> error(path, "se esperaba un literal (texto, número o booleano), no null");
      case String s -> new Literal.Text(s);
      case Boolean b -> new Literal.Bool(b);
      case BigDecimal d -> new Literal.Number(d);
      case Number n -> {
        try {
          yield new Literal.Number(new BigDecimal(n.toString()));
        } catch (NumberFormatException e) {
          yield error(path, "número no válido: " + n);
        }
      }
      default -> error(path, "se esperaba un literal (texto, número o booleano)");
    };
  }

  /** eq / ne / in: tipo del literal compatible con el campo. */
  private boolean checkEquality(FieldRef field, Literal value, String path) {
    return switch (field.type()) {
      case STRING -> require(value instanceof Literal.Text, path, field, "un texto");
      case BOOLEAN -> require(value instanceof Literal.Bool, path, field, "un booleano");
      case DATETIME -> requireInstant(value, path, field);
      case SET -> require(false, path, field, "'contains' (no admite eq/ne/in/gt/lt)");
      case DYNAMIC -> true;
    };
  }

  /** gt / lt / between: solo fechas y números. */
  private boolean checkOrdered(FieldRef field, Literal value, String path) {
    return switch (field.type()) {
      case DATETIME -> requireInstant(value, path, field);
      case DYNAMIC ->
          require(
              value instanceof Literal.Number
                  || (value instanceof Literal.Text t && Temporal.date(t.value()).isPresent()),
              path,
              field,
              "un número o una fecha yyyy-MM-dd");
      default ->
          require(false, path, field, "un campo numérico o de fecha (gt/lt/between no aplican)");
    };
  }

  private boolean requireInstant(Literal value, String path, FieldRef field) {
    return require(
        value instanceof Literal.Text t && Temporal.instant(t.value()).isPresent(),
        path,
        field,
        "una fecha yyyy-MM-dd o un instante ISO-8601");
  }

  private boolean require(boolean ok, String path, FieldRef field, String expected) {
    if (!ok) {
      error(path, "el campo '" + field.path() + "' requiere " + expected);
    }
    return ok;
  }

  private <T> T error(String path, String message) {
    errors.add(path + ": " + message);
    return null;
  }

  private static String operators() {
    return String.join(", ", OPERATORS.stream().sorted().toList());
  }
}
