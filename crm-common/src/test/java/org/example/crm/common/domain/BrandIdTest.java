package org.example.crm.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BrandIdTest {

  @Test
  void parsesUuid() {
    UUID uuid = UUID.randomUUID();

    assertThat(BrandId.of(uuid.toString()).value()).isEqualTo(uuid);
    assertThat(BrandId.of(uuid.toString())).hasToString(uuid.toString());
  }

  @Test
  void rejectsInvalidValues() {
    assertThatThrownBy(() -> BrandId.of("marca-a")).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new BrandId(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void randomIdsDiffer() {
    assertThat(BrandId.random()).isNotEqualTo(BrandId.random());
  }

  @Test
  void exceptionHierarchy() {
    assertThat(new NotFoundException("x")).isInstanceOf(DomainException.class).hasMessage("x");
    assertThat(new ConflictException("y")).isInstanceOf(DomainException.class).hasMessage("y");
  }
}
