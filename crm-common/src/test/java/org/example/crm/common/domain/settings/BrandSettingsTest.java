package org.example.crm.common.domain.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.DomainException;
import org.junit.jupiter.api.Test;

class BrandSettingsTest {

  private final Brand brand = Brand.create("marca-a", "Marca A", "marca-a.example.test", "es");
  private final BrandSettings defaults = BrandSettings.defaults(brand);

  private BrandSettings with(
      Map<NotificationType, NotificationSettings> n,
      List<String> languages,
      Map<DeliveryChannel, Integer> retries,
      int threshold,
      Map<DeliveryChannel, List<String>> providers) {
    return new BrandSettings(n, languages, retries, threshold, providers);
  }

  @Test
  void documentedDefaults() {
    assertThat(defaults.languages()).containsExactly("es");
    assertThat(defaults.defaultLanguage()).isEqualTo("es");
    assertThat(defaults.loginLockThreshold()).isEqualTo(5);
    assertThat(defaults.maxRetries(DeliveryChannel.EMAIL)).isEqualTo(3);
    assertThat(defaults.maxRetries(DeliveryChannel.SMS)).isEqualTo(3);
    assertThat(defaults.maxRetries(DeliveryChannel.PUSH)).isEqualTo(2);
    assertThat(defaults.maxRetries(DeliveryChannel.INBOX)).isZero();
    assertThat(defaults.providers(DeliveryChannel.EMAIL)).isEmpty();
    NotificationSettings reg = defaults.notification(NotificationType.REGISTRATION_VERIFICATION);
    assertThat(reg.senderName()).isEqualTo("Marca A");
    assertThat(reg.senderAddress()).isEqualTo("no-reply@marca-a.example.test");
    assertThat(defaults.notifications()).hasSize(NotificationType.values().length);
    assertThat(
            BrandSettings.defaults(Brand.create("marca-b", "Marca B", null, "en"))
                .notification(NotificationType.PASSWORD_RESET)
                .senderAddress())
        .isEqualTo("no-reply@example.invalid");
  }

  @Test
  void acceptsValidCustomSettings() {
    Map<DeliveryChannel, List<String>> providers = new EnumMap<>(defaults.activeProviders());
    providers.put(DeliveryChannel.SMS, List.of("gateway-1", "gateway-2"));
    Map<DeliveryChannel, Integer> retries = new EnumMap<>(defaults.maxRetries());
    retries.put(DeliveryChannel.SMS, 10);

    BrandSettings s =
        with(defaults.notifications(), List.of("es", "en", "es-ES"), retries, 20, providers);

    assertThat(s.providers(DeliveryChannel.SMS)).containsExactly("gateway-1", "gateway-2");
    assertThat(s.maxRetries(DeliveryChannel.SMS)).isEqualTo(10);
    assertThat(s.languages()).containsExactly("es", "en", "es-ES");
  }

  @Test
  void validatesRangesAndFormats() {
    var n = defaults.notifications();
    var r = defaults.maxRetries();
    var p = defaults.activeProviders();
    assertThatThrownBy(() -> with(n, List.of(), r, 5, p)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> with(n, List.of("ES"), r, 5, p)).hasMessageContaining("Idioma");
    assertThatThrownBy(() -> with(n, List.of("es", "es"), r, 5, p))
        .hasMessageContaining("repetidos");
    assertThatThrownBy(() -> with(n, List.of("es"), r, 0, p)).hasMessageContaining("umbral");
    assertThatThrownBy(() -> with(n, List.of("es"), r, 21, p)).hasMessageContaining("umbral");

    Map<DeliveryChannel, Integer> badRetries = new EnumMap<>(r);
    badRetries.put(DeliveryChannel.EMAIL, 11);
    assertThatThrownBy(() -> with(n, List.of("es"), badRetries, 5, p))
        .hasMessageContaining("EMAIL");
    Map<DeliveryChannel, Integer> missing = new EnumMap<>(r);
    missing.remove(DeliveryChannel.PUSH);
    assertThatThrownBy(() -> with(n, List.of("es"), missing, 5, p))
        .hasMessageContaining("maxRetries.PUSH");

    Map<DeliveryChannel, List<String>> badProviders = new EnumMap<>(p);
    badProviders.put(DeliveryChannel.EMAIL, List.of("smtp-1", "smtp-1"));
    assertThatThrownBy(() -> with(n, List.of("es"), r, 5, badProviders))
        .hasMessageContaining("Proveedores");
    badProviders.put(DeliveryChannel.EMAIL, List.of("SMTP 1"));
    assertThatThrownBy(() -> with(n, List.of("es"), r, 5, badProviders))
        .hasMessageContaining("Proveedores");
    Map<DeliveryChannel, List<String>> nullList = new HashMap<>(p);
    nullList.put(DeliveryChannel.EMAIL, null);
    assertThat(with(n, List.of("es"), r, 5, nullList).providers(DeliveryChannel.EMAIL)).isEmpty();

    Map<NotificationType, NotificationSettings> nullNotification = new HashMap<>(n);
    nullNotification.put(NotificationType.PASSWORD_RESET, null);
    assertThatThrownBy(() -> with(nullNotification, List.of("es"), r, 5, p))
        .hasMessageContaining("PASSWORD_RESET");
    assertThatThrownBy(() -> with(null, List.of("es"), r, 5, p))
        .hasMessageContaining("notifications");
  }

  @Test
  void validatesNotificationSettings() {
    assertThatThrownBy(() -> new NotificationSettings(" ", "a@b.test", "x"))
        .isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> new NotificationSettings("Marca A", "no-es-email", "x"))
        .hasMessageContaining("remitente");
    assertThatThrownBy(() -> new NotificationSettings("Marca A", "a@b.test", ""))
        .hasMessageContaining("asunto");
  }
}
