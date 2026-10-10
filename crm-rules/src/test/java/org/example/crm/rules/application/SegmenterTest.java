package org.example.crm.rules.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.example.crm.common.application.tenant.TenantContext;
import org.example.crm.common.application.tenant.TenantNotSetException;
import org.example.crm.common.domain.BrandId;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.rules.domain.RuleFixtures;
import org.junit.jupiter.api.Test;

class SegmenterTest {

  @Test
  void runsTranslatedQueryInCurrentBrand() {
    ConsumerId id = ConsumerId.random();
    BrandId brand = BrandId.random();
    Segmenter segmenter =
        new Segmenter(
            new TestSqlCatalog(),
            q -> {
              assertThat(q.params().get(0)).isEqualTo(brand.value());
              return List.of(id);
            });
    var rule = RuleFixtures.rule("{\"exists\":{\"field\":\"segment\"}}");

    assertThat(TenantContext.callAs(brand, () -> segmenter.matchingConsumers(rule)))
        .containsExactly(id);
    assertThatThrownBy(() -> segmenter.sql(rule)).isInstanceOf(TenantNotSetException.class);
  }
}
