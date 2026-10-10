package org.example.crm.rules.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.PreferenceValue;

/**
 * Evaluación en memoria de una regla contra un {@link RuleContext}.
 *
 * <p>Comparaciones: textos sensibles a mayúsculas; números por valor ({@code 1 == 1.0}); fechas de
 * preferencias contra literales {@code yyyy-MM-dd}; {@code registeredAt} contra fecha (inicio del
 * día UTC) o instante. Un valor ausente o de tipo incompatible hace la comparación falsa ({@code
 * ne} = {@code not eq}).
 */
public final class RuleEvaluator {

  private RuleEvaluator() {}

  public static boolean evaluate(Rule rule, RuleContext ctx) {
    return switch (rule) {
      case Rule.And and -> and.rules().stream().allMatch(r -> evaluate(r, ctx));
      case Rule.Or or -> or.rules().stream().anyMatch(r -> evaluate(r, ctx));
      case Rule.Not not -> !evaluate(not.rule(), ctx);
      case Rule.Compare c ->
          switch (c.op()) {
            case EQ -> equal(value(c.field(), ctx), c.value());
            case NE -> !equal(value(c.field(), ctx), c.value());
            case GT -> compare(value(c.field(), ctx), c.value()).map(x -> x > 0).orElse(false);
            case LT -> compare(value(c.field(), ctx), c.value()).map(x -> x < 0).orElse(false);
          };
      case Rule.In in -> {
        Object v = value(in.field(), ctx);
        yield in.values().stream().anyMatch(l -> equal(v, l));
      }
      case Rule.Between b -> {
        Object v = value(b.field(), ctx);
        yield compare(v, b.from()).map(x -> x >= 0).orElse(false)
            && compare(v, b.to()).map(x -> x <= 0).orElse(false);
      }
      case Rule.Exists e -> value(e.field(), ctx) != null;
      case Rule.Contains c -> contains(value(c.field(), ctx), ((Literal.Text) c.value()).value());
      case Rule.SubscribedTo s -> ctx.subscriptions().contains(s.serviceCode());
      case Rule.EventOccurred e -> ctx.eventOccurred(e.type(), e.within());
    };
  }

  /** Valor nativo del campo en el contexto, o {@code null} si no existe. */
  static Object value(FieldRef field, RuleContext ctx) {
    return switch (field.kind()) {
      case PROFILE -> ctx.profile().get(field.name());
      case ATTRIBUTE -> ctx.attributes().get(field.name());
      case PREFERENCE -> {
        PreferenceValue p = ctx.preferences().get(field.name());
        yield p == null ? null : p.raw();
      }
      case CONSENT -> ctx.allowedChannels().contains(ConsentChannel.valueOf(field.name()));
      case SUBSCRIPTIONS -> ctx.subscriptions();
    };
  }

  private static boolean equal(Object actual, Literal literal) {
    if (actual == null) {
      return false;
    }
    return switch (literal) {
      case Literal.Text t ->
          switch (actual) {
            case String s -> s.equals(t.value());
            case Instant i -> Temporal.instant(t.value()).map(i::equals).orElse(false);
            case LocalDate d -> Temporal.date(t.value()).map(d::equals).orElse(false);
            default -> false;
          };
      case Literal.Number n -> actual instanceof BigDecimal d && d.compareTo(n.value()) == 0;
      case Literal.Bool b -> actual instanceof Boolean x && x == b.value();
    };
  }

  private static Optional<Integer> compare(Object actual, Literal literal) {
    if (actual == null) {
      return Optional.empty();
    }
    return switch (literal) {
      case Literal.Number n ->
          actual instanceof BigDecimal d ? Optional.of(d.compareTo(n.value())) : Optional.empty();
      case Literal.Text t ->
          switch (actual) {
            case Instant i -> Temporal.instant(t.value()).map(i::compareTo);
            case LocalDate d -> Temporal.date(t.value()).map(d::compareTo);
            default -> Optional.empty();
          };
      case Literal.Bool b -> Optional.empty();
    };
  }

  private static boolean contains(Object actual, String needle) {
    return switch (actual) {
      case null -> false;
      case String s -> s.contains(needle);
      case Collection<?> c -> c.contains(needle);
      default -> false;
    };
  }
}
