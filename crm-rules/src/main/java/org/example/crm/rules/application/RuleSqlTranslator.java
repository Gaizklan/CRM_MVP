package org.example.crm.rules.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.application.SegmentationSqlCatalog;
import org.example.crm.consumer.application.SegmentationSqlCatalog.ProfileColumn;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.PreferenceType;
import org.example.crm.rules.domain.FieldRef;
import org.example.crm.rules.domain.Literal;
import org.example.crm.rules.domain.Rule;

/**
 * Traduce un {@link Rule} a una condición SQL parametrizada sobre el consumidor {@code c},
 * reproduciendo exactamente la semántica de {@code RuleEvaluator}:
 *
 * <ul>
 *   <li>Toda hoja produce un booleano nunca nulo ({@code COALESCE(..., FALSE)} o {@code EXISTS}),
 *       de modo que {@code NOT} y {@code ne} se comportan como en memoria (lógica de dos valores).
 *   <li>Los valores siempre van como parámetros; el texto SQL solo contiene nombres de columna
 *       publicados por {@link SegmentationSqlCatalog}.
 *   <li>Lo que no puede expresarse en SQL se rechaza con {@link RuleNotTranslatableException}.
 * </ul>
 */
public final class RuleSqlTranslator {

  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final SqlFragment TRUE = SqlFragment.of("TRUE");

  private final SegmentationSqlCatalog catalog;
  private final List<String> problems = new ArrayList<>();

  private RuleSqlTranslator(SegmentationSqlCatalog catalog) {
    this.catalog = catalog;
  }

  /** Condición SQL equivalente a la regla. */
  public static SqlFragment toCondition(Rule rule, SegmentationSqlCatalog catalog) {
    RuleSqlTranslator t = new RuleSqlTranslator(catalog);
    SqlFragment sql = t.translate(rule);
    if (!t.problems.isEmpty()) {
      throw new RuleNotTranslatableException(t.problems);
    }
    return sql;
  }

  /** Consulta completa de identificadores de consumidores de la Marca que cumplen la regla. */
  public static SqlFragment toQuery(Rule rule, BrandId brand, SegmentationSqlCatalog catalog) {
    return catalog.selectConsumerIds(brand, toCondition(rule, catalog));
  }

  private SqlFragment translate(Rule rule) {
    return switch (rule) {
      case Rule.And and ->
          SqlFragment.join(" AND ", and.rules().stream().map(this::translate).toList());
      case Rule.Or or ->
          SqlFragment.join(" OR ", or.rules().stream().map(this::translate).toList());
      case Rule.Not not -> SqlFragment.template("(NOT %s)", List.of(), translate(not.rule()));
      case Rule.Compare c ->
          switch (c.op()) {
            case EQ -> equal(c.field(), List.of(c.value()));
            case NE ->
                SqlFragment.template("(NOT %s)", List.of(), equal(c.field(), List.of(c.value())));
            case GT -> ordered(c.field(), ">", c.value());
            case LT -> ordered(c.field(), "<", c.value());
          };
      case Rule.In in -> equal(in.field(), in.values());
      case Rule.Between b -> between(b.field(), b.from(), b.to());
      case Rule.Exists e -> exists(e.field());
      case Rule.Contains c -> contains(c.field(), ((Literal.Text) c.value()).value());
      case Rule.SubscribedTo s -> catalog.subscribedTo(s.serviceCode());
      case Rule.EventOccurred e ->
          unsupported(
              "eventOccurred("
                  + e.type()
                  + ") requiere el registro de eventos de usuario y solo se"
                  + " evalúa en memoria");
    };
  }

  // ---- eq / in ----

  private SqlFragment equal(FieldRef field, List<Literal> values) {
    return switch (field.kind()) {
      case PROFILE -> {
        Optional<ProfileColumn> column = comparable(field);
        if (column.isEmpty()) {
          yield TRUE;
        }
        List<Object> params = new ArrayList<>();
        for (Literal value : values) {
          params.add(
              field.name().equals("registeredAt")
                  ? timestamp(value)
                  : catalog.bindProfileValue(field.name(), ((Literal.Text) value).value()));
        }
        yield SqlFragment.of(
            "COALESCE("
                + column.get().expression()
                + " IN ("
                + markers(params.size())
                + "), FALSE)",
            params.toArray());
      }
      case ATTRIBUTE -> {
        List<Object> params = values.stream().map(Literal::raw).toList();
        yield catalog.attributeExists(
            field.name(),
            SqlFragment.of(
                catalog.attributeValueColumn() + " IN (" + markers(params.size()) + ")",
                params.toArray()));
      }
      case PREFERENCE ->
          catalog.preferenceExists(
              field.name(),
              SqlFragment.join(" OR ", values.stream().map(this::preferenceEquals).toList()));
      case CONSENT -> {
        ConsentChannel channel = ConsentChannel.valueOf(field.name());
        List<SqlFragment> options = new ArrayList<>();
        for (Literal value : values) {
          boolean granted = ((Literal.Bool) value).value();
          SqlFragment allowed = catalog.consentAllowed(channel);
          options.add(granted ? allowed : SqlFragment.template("(NOT %s)", List.of(), allowed));
        }
        yield SqlFragment.join(" OR ", options);
      }
      case SUBSCRIPTIONS -> unsupported("'subscriptions' solo admite contains/exists");
    };
  }

  private SqlFragment preferenceEquals(Literal value) {
    return switch (value) {
      case Literal.Number n -> typed(PreferenceType.NUMBER, " = ?", n.value());
      case Literal.Bool b -> typed(PreferenceType.BOOLEAN, " = ?", b.value());
      case Literal.Text t -> {
        SqlFragment text = typed(PreferenceType.STRING, " = ?", t.value());
        Optional<LocalDate> date = date(t.value());
        yield date.isEmpty()
            ? text
            : SqlFragment.join(
                " OR ", List.of(text, typed(PreferenceType.DATE, " = ?", date.get())));
      }
    };
  }

  // ---- gt / lt / between ----

  private SqlFragment ordered(FieldRef field, String operator, Literal value) {
    return switch (field.kind()) {
      case PROFILE ->
          comparable(field)
              .map(
                  col ->
                      SqlFragment.of(
                          "COALESCE(" + col.expression() + " " + operator + " ?, FALSE)",
                          timestamp(value)))
              .orElse(TRUE);
      case PREFERENCE -> catalog.preferenceExists(field.name(), preferenceOrdered(operator, value));
      default -> unsupported("gt/lt no aplica al campo " + field.path());
    };
  }

  private SqlFragment preferenceOrdered(String operator, Literal value) {
    return switch (value) {
      case Literal.Number n -> typed(PreferenceType.NUMBER, " " + operator + " ?", n.value());
      case Literal.Text t ->
          typed(PreferenceType.DATE, " " + operator + " ?", date(t.value()).orElseThrow());
      case Literal.Bool b -> unsupported("gt/lt no aplica a booleanos");
    };
  }

  private SqlFragment between(FieldRef field, Literal from, Literal to) {
    return switch (field.kind()) {
      case PROFILE ->
          comparable(field)
              .map(
                  col ->
                      SqlFragment.of(
                          "COALESCE(" + col.expression() + " BETWEEN ? AND ?, FALSE)",
                          timestamp(from),
                          timestamp(to)))
              .orElse(TRUE);
      case PREFERENCE -> {
        PreferenceType type =
            from instanceof Literal.Number ? PreferenceType.NUMBER : PreferenceType.DATE;
        Object lo =
            type == PreferenceType.NUMBER ? from.raw() : date((String) from.raw()).orElseThrow();
        Object hi =
            type == PreferenceType.NUMBER ? to.raw() : date((String) to.raw()).orElseThrow();
        yield catalog.preferenceExists(
            field.name(),
            SqlFragment.of(
                "("
                    + catalog.preferenceTypeColumn()
                    + " = ? AND "
                    + catalog.preferenceColumn(type)
                    + " BETWEEN ? AND ?)",
                type.name(),
                lo,
                hi));
      }
      default -> unsupported("between no aplica al campo " + field.path());
    };
  }

  // ---- exists / contains ----

  private SqlFragment exists(FieldRef field) {
    return switch (field.kind()) {
      case PROFILE ->
          profile(field)
              .map(col -> SqlFragment.of("(" + col.expression() + " IS NOT NULL)"))
              .orElse(TRUE);
      case ATTRIBUTE -> catalog.attributeExists(field.name(), TRUE);
      case PREFERENCE -> catalog.preferenceExists(field.name(), TRUE);
        // En memoria, consent.<CANAL> y subscriptions siempre tienen valor.
      case CONSENT, SUBSCRIPTIONS -> TRUE;
    };
  }

  private SqlFragment contains(FieldRef field, String needle) {
    return switch (field.kind()) {
      case PROFILE -> {
        Optional<ProfileColumn> column = profile(field);
        if (column.isPresent() && !column.get().substringSearch()) {
          yield unsupported(
              "'contains' sobre " + field.path() + " no es posible en SQL (columna no buscable)");
        }
        yield column
            .map(
                col ->
                    SqlFragment.of(
                        "COALESCE(strpos(" + col.expression() + ", ?) > 0, FALSE)", needle))
            .orElse(TRUE);
      }
      case ATTRIBUTE ->
          catalog.attributeExists(
              field.name(),
              SqlFragment.of("strpos(" + catalog.attributeValueColumn() + ", ?) > 0", needle));
      case PREFERENCE ->
          catalog.preferenceExists(
              field.name(),
              SqlFragment.of(
                  "("
                      + catalog.preferenceTypeColumn()
                      + " = ? AND strpos("
                      + catalog.preferenceColumn(PreferenceType.STRING)
                      + ", ?) > 0)",
                  PreferenceType.STRING.name(),
                  needle));
      case SUBSCRIPTIONS -> catalog.subscribedTo(needle);
      case CONSENT -> unsupported("'contains' no aplica a consentimientos");
    };
  }

  // ---- utilidades ----

  private SqlFragment typed(PreferenceType type, String comparison, Object value) {
    return SqlFragment.of(
        "("
            + catalog.preferenceTypeColumn()
            + " = ? AND "
            + catalog.preferenceColumn(type)
            + comparison
            + ")",
        type.name(),
        value);
  }

  /** Columna de perfil que admite comparación por valor (no cifrada de forma no determinista). */
  private Optional<ProfileColumn> comparable(FieldRef field) {
    Optional<ProfileColumn> column = profile(field);
    if (column.isPresent() && !column.get().equality()) {
      unsupported("el campo " + field.path() + " está cifrado y solo admite 'exists' en SQL");
      return Optional.empty();
    }
    return column;
  }

  private Optional<ProfileColumn> profile(FieldRef field) {
    Optional<ProfileColumn> column = catalog.profileColumn(field.name());
    if (column.isEmpty()) {
      unsupported("el campo " + field.path() + " no es consultable en SQL");
    }
    return column;
  }

  private static OffsetDateTime timestamp(Literal value) {
    String text = ((Literal.Text) value).value();
    Instant instant =
        date(text)
            .map(d -> d.atStartOfDay(ZoneOffset.UTC).toInstant())
            .orElseGet(() -> Instant.parse(text));
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Optional<LocalDate> date(String text) {
    if (!DATE.matcher(text).matches()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(text));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  private static String markers(int count) {
    return String.join(", ", Collections.nCopies(count, "?"));
  }

  private SqlFragment unsupported(String reason) {
    problems.add(reason);
    return TRUE;
  }
}
