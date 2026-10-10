package org.example.crm.common.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fragmento SQL parametrizado: texto con marcadores {@code ?} y sus valores en orden. Los valores
 * nunca se concatenan en el texto.
 */
public record SqlFragment(String sql, List<Object> params) {

  public SqlFragment {
    Objects.requireNonNull(sql, "sql");
    params = List.copyOf(params);
    long markers = sql.chars().filter(ch -> ch == '?').count();
    if (markers != params.size()) {
      throw new IllegalArgumentException(
          "El fragmento tiene %d marcadores y %d parámetros: %s"
              .formatted(markers, params.size(), sql));
    }
  }

  public static SqlFragment of(String sql, Object... params) {
    return new SqlFragment(sql, Arrays.asList(params));
  }

  /**
   * Sustituye cada {@code %s} de la plantilla por un fragmento, concatenando sus parámetros en
   * orden. La plantilla puede tener además marcadores {@code ?} propios, cuyos valores se pasan en
   * {@code ownParams} y deben aparecer antes que el primer {@code %s}.
   */
  public static SqlFragment template(
      String template, List<Object> ownParams, SqlFragment... parts) {
    String[] pieces = template.split("%s", -1);
    if (pieces.length != parts.length + 1) {
      throw new IllegalArgumentException("Plantilla y fragmentos no coinciden: " + template);
    }
    StringBuilder sql = new StringBuilder(pieces[0]);
    List<Object> params = new ArrayList<>(ownParams);
    for (int i = 0; i < parts.length; i++) {
      sql.append(parts[i].sql()).append(pieces[i + 1]);
      params.addAll(parts[i].params());
    }
    return new SqlFragment(sql.toString(), params);
  }

  /** Une fragmentos con un separador (p. ej. {@code " AND "}), entre paréntesis. */
  public static SqlFragment join(String separator, List<SqlFragment> parts) {
    StringBuilder sql = new StringBuilder("(");
    List<Object> params = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) {
        sql.append(separator);
      }
      sql.append(parts.get(i).sql());
      params.addAll(parts.get(i).params());
    }
    return new SqlFragment(sql.append(')').toString(), params);
  }
}
