package org.example.crm.common.application.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.crm.common.domain.BrandId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

  private final BrandId brandA = BrandId.random();
  private final BrandId brandB = BrandId.random();

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void emptyByDefault() {
    assertThat(TenantContext.current()).isEmpty();
    assertThat(TenantContext.isRoot()).isFalse();
    assertThatThrownBy(TenantContext::require).isInstanceOf(TenantNotSetException.class);
  }

  @Test
  void setAndClear() {
    TenantContext.set(brandA);
    assertThat(TenantContext.require()).isEqualTo(brandA);
    TenantContext.clear();
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void callAsRestoresPreviousContext() {
    TenantContext.set(brandA);

    BrandId inside = TenantContext.callAs(brandB, TenantContext::require);

    assertThat(inside).isEqualTo(brandB);
    assertThat(TenantContext.require()).isEqualTo(brandA);
  }

  @Test
  void runAsRestoresEmptyContextEvenOnException() {
    assertThatThrownBy(
            () ->
                TenantContext.runAs(
                    brandA,
                    () -> {
                      throw new IllegalStateException("boom");
                    }))
        .hasMessage("boom");
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void rootScopeHasNoBrand() {
    boolean root = TenantContext.callAsRoot(TenantContext::isRoot);

    assertThat(root).isTrue();
    assertThat(TenantContext.isRoot()).isFalse();
    assertThat(TenantContext.callAsRoot(TenantContext::current)).isEmpty();
  }
}
