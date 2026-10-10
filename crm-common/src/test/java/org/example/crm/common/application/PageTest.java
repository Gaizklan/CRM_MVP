package org.example.crm.common.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;

class PageTest {

  @Test
  void validatesPageQuery() {
    assertThat(new PageQuery(0, 20).size()).isEqualTo(20);
    assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new PageQuery(0, 201)).isInstanceOf(DomainException.class);
  }

  @Test
  void computesTotalPagesAndMaps() {
    PageResult<Integer> page = new PageResult<>(List.of(1, 2), 0, 2, 5);

    assertThat(page.totalPages()).isEqualTo(3);
    assertThat(page.map(i -> "n" + i).content()).containsExactly("n1", "n2");
    assertThat(new PageResult<>(List.of(), 0, 0, 0).totalPages()).isZero();
  }
}
