package org.example.crm.common.infrastructure.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class OutboundTlsPolicyTest {

  private final OutboundTlsPolicy strict = new OutboundTlsPolicy(false);
  private final OutboundTlsPolicy dev = new OutboundTlsPolicy(true);

  @Test
  void httpsIsRequiredForProviders() {
    assertThat(strict.requireHttps(URI.create("https://sms.example.test/send"))).isNotNull();
    assertThatThrownBy(() -> strict.requireHttps(URI.create("http://sms.example.test/send")))
        .hasMessageContaining("TLS obligatorio");
    assertThatThrownBy(() -> strict.requireHttps(URI.create("http://localhost:8089/sms/send")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> strict.requireHttps(URI.create("/relative")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void devAllowsPlainHttpOnlyToLocalhost() {
    assertThat(dev.requireHttps(URI.create("http://localhost:8089/sms/send"))).isNotNull();
    assertThatThrownBy(() -> dev.requireHttps(URI.create("http://sms.example.test/send")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void smtpRequiresStartTls() {
    assertThat(strict.smtpProperties("smtp.example.test"))
        .containsEntry("mail.smtp.starttls.enable", "true")
        .containsEntry("mail.smtp.starttls.required", "true")
        .containsEntry("mail.smtp.ssl.checkserveridentity", "true");
    assertThat(strict.smtpProperties("localhost"))
        .containsEntry("mail.smtp.starttls.required", "true");
    assertThat(dev.smtpProperties("localhost")).containsEntry("mail.smtp.starttls.enable", "false");
    assertThat(dev.smtpProperties(null)).containsEntry("mail.smtp.starttls.required", "true");
  }
}
