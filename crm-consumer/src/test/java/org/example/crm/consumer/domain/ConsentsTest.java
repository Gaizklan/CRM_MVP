package org.example.crm.consumer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ConsentsTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
  private final BrandId brand = BrandId.random();
  private final ConsumerId consumer = ConsumerId.random();

  private Consents withProgram() {
    Consents c = Consents.empty(brand, consumer);
    c.change(ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer", clock);
    c.pullChanges();
    c.pullEvents();
    return c;
  }

  @ParameterizedTest
  @EnumSource(value = ConsentChannel.class, names = "CRM_PROGRAM", mode = EnumSource.Mode.EXCLUDE)
  void noRecordMeansNotAllowed(ConsentChannel channel) {
    Consents c = withProgram();

    assertThat(c.isAllowed(channel)).isFalse();
    assertThat(c.isGranted(channel)).isFalse();
    assertThat(c.status(channel)).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = ConsentChannel.class, names = "CRM_PROGRAM", mode = EnumSource.Mode.EXCLUDE)
  void optInAndOptOutPerChannelAreAudited(ConsentChannel channel) {
    Consents c = withProgram();

    c.change(channel, true, ConsentSource.ADMIN, "admin:ana", clock);
    assertThat(c.isAllowed(channel)).isTrue();
    c.change(channel, false, ConsentSource.EMAIL, "consumer", clock);
    assertThat(c.isAllowed(channel)).isFalse();

    var changes = c.pullChanges();
    assertThat(changes).hasSize(2);
    assertThat(changes.get(0))
        .extracting(
            ConsentChange::channel,
            ConsentChange::granted,
            ConsentChange::source,
            ConsentChange::actor,
            ConsentChange::occurredAt)
        .containsExactly(channel, true, ConsentSource.ADMIN, "admin:ana", clock.instant());
    assertThat(changes.get(1).source()).isEqualTo(ConsentSource.EMAIL);
    assertThat(c.pullEvents()).hasSize(2).allMatch(e -> e instanceof ConsentChanged);
  }

  @ParameterizedTest
  @EnumSource(value = ConsentChannel.class, names = "CRM_PROGRAM", mode = EnumSource.Mode.EXCLUDE)
  void channelRequiresProgramOptIn(ConsentChannel channel) {
    Consents c = Consents.empty(brand, consumer);

    c.change(channel, true, ConsentSource.IMPORT, "import:lote-1", clock);

    assertThat(c.isGranted(channel)).isTrue();
    assertThat(c.isAllowed(channel)).isFalse();
    c.change(ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer", clock);
    assertThat(c.isAllowed(channel)).isTrue();
  }

  @Test
  void programOptOutImpliesOptOutOfAllChannels() {
    Consents c = withProgram();
    c.change(ConsentChannel.EMAIL, true, ConsentSource.WEB, "consumer", clock);
    c.change(ConsentChannel.SMS, true, ConsentSource.WEB, "consumer", clock);
    c.change(ConsentChannel.PUSH, true, ConsentSource.WEB, "consumer", clock);
    c.pullChanges();

    c.change(ConsentChannel.CRM_PROGRAM, false, ConsentSource.EMAIL, "consumer", clock);

    for (ConsentChannel channel : ConsentChannel.values()) {
      assertThat(c.isAllowed(channel)).as(channel.name()).isFalse();
    }
    assertThat(c.isGranted(ConsentChannel.EMAIL)).isFalse();
    var changes = c.pullChanges();
    assertThat(changes)
        .extracting(ConsentChange::channel)
        .containsExactly(
            ConsentChannel.CRM_PROGRAM,
            ConsentChannel.EMAIL,
            ConsentChannel.SMS,
            ConsentChannel.PUSH);
    assertThat(changes).allMatch(ch -> !ch.granted() && ch.source() == ConsentSource.EMAIL);

    c.change(ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer", clock);
    assertThat(c.isAllowed(ConsentChannel.EMAIL)).as("no se reactivan los canales").isFalse();
  }

  @Test
  void repeatingCurrentStateIsNotRecorded() {
    Consents c = withProgram();

    c.change(ConsentChannel.CRM_PROGRAM, true, ConsentSource.WEB, "consumer", clock);
    c.change(ConsentChannel.SMS, false, ConsentSource.WEB, "consumer", clock);

    assertThat(c.pullChanges()).isEmpty();
    assertThat(c.pullEvents()).isEmpty();
  }

  @Test
  void restoreAndAccessors() {
    Consents c =
        Consents.restore(
            brand,
            consumer,
            Map.of(
                ConsentChannel.CRM_PROGRAM,
                new ConsentStatus(true, ConsentSource.IMPORT, clock.instant()),
                ConsentChannel.MMS,
                new ConsentStatus(true, ConsentSource.IMPORT, clock.instant())));

    assertThat(c.isAllowed(ConsentChannel.MMS)).isTrue();
    assertThat(c.isAllowed(ConsentChannel.CRM_PROGRAM)).isTrue();
    assertThat(c.current()).hasSize(2);
    assertThat(c.brandId()).isEqualTo(brand);
    assertThat(c.consumerId()).isEqualTo(consumer);
    assertThat(ConsentChannel.CRM_PROGRAM.isProgram()).isTrue();
  }

  @Test
  void actorIsRequired() {
    Consents c = withProgram();

    assertThatThrownBy(() -> c.change(ConsentChannel.SMS, true, ConsentSource.WEB, " ", clock))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> c.change(ConsentChannel.SMS, true, ConsentSource.WEB, null, clock))
        .isInstanceOf(DomainException.class);
  }
}
