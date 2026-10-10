package org.example.crm.common.application.tenant;

import java.util.Optional;
import java.util.function.Supplier;
import org.example.crm.common.domain.BrandId;

/**
 * Contexto de Marca (tenant) del hilo actual. Se resuelve por petición (filtro web) o lo fija
 * explícitamente un proceso en segundo plano. Toda consulta a entidades de negocio se filtra por la
 * Marca de este contexto; sin contexto, las consultas no devuelven datos (fallo cerrado).
 *
 * <p>El ámbito <em>raíz</em> ({@link #callAsRoot}) permite operaciones de sistema que abarcan todas
 * las Marcas (p. ej. procesos de plataforma); debe usarse solo en código de infraestructura
 * explícitamente revisado.
 */
public final class TenantContext {

  private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  /** Marca del contexto actual, si la hay. */
  public static Optional<BrandId> current() {
    Scope scope = CURRENT.get();
    return scope == null ? Optional.empty() : Optional.ofNullable(scope.brandId());
  }

  /** Marca del contexto actual o {@link TenantNotSetException} si no se ha resuelto ninguna. */
  public static BrandId require() {
    return current().orElseThrow(TenantNotSetException::new);
  }

  /** Indica si el hilo actual opera en ámbito raíz (todas las Marcas). */
  public static boolean isRoot() {
    Scope scope = CURRENT.get();
    return scope != null && scope.root();
  }

  /** Fija la Marca del hilo actual. Debe ir acompañado de {@link #clear()} en un bloque finally. */
  public static void set(BrandId brandId) {
    CURRENT.set(new Scope(java.util.Objects.requireNonNull(brandId, "brandId"), false));
  }

  public static void clear() {
    CURRENT.remove();
  }

  /** Ejecuta {@code action} con la Marca indicada y restaura el contexto anterior. */
  public static <T> T callAs(BrandId brandId, Supplier<T> action) {
    java.util.Objects.requireNonNull(brandId, "brandId");
    return within(new Scope(brandId, false), action);
  }

  public static void runAs(BrandId brandId, Runnable action) {
    callAs(
        brandId,
        () -> {
          action.run();
          return null;
        });
  }

  /**
   * Ejecuta {@code action} en ámbito raíz (sin filtro de Marca) y restaura el contexto anterior.
   */
  public static <T> T callAsRoot(Supplier<T> action) {
    return within(new Scope(null, true), action);
  }

  private static <T> T within(Scope scope, Supplier<T> action) {
    Scope previous = CURRENT.get();
    CURRENT.set(scope);
    try {
      return action.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }

  private record Scope(BrandId brandId, boolean root) {}
}
