package org.example.crm.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlFragmentTest {

  @Test
  void validatesMarkersAgainstParams() {
    assertThat(SqlFragment.of("x = ?", 1).params()).containsExactly(1);
    assertThatThrownBy(() -> SqlFragment.of("x = ?")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void templateAndJoinKeepParameterOrder() {
    SqlFragment a = SqlFragment.of("a = ?", "A");
    SqlFragment b = SqlFragment.of("b IN (?, ?)", 1, 2);

    SqlFragment joined = SqlFragment.join(" AND ", List.of(a, b));
    SqlFragment t = SqlFragment.template("EXISTS (k = ? AND %s)", List.of("key"), joined);

    assertThat(joined.sql()).isEqualTo("(a = ? AND b IN (?, ?))");
    assertThat(t.sql()).isEqualTo("EXISTS (k = ? AND (a = ? AND b IN (?, ?)))");
    assertThat(t.params()).containsExactly("key", "A", 1, 2);
    assertThatThrownBy(() -> SqlFragment.template("%s %s", List.of(), a))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
