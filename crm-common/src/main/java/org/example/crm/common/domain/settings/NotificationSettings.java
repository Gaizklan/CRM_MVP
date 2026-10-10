package org.example.crm.common.domain.settings;

import java.util.regex.Pattern;
import org.example.crm.common.domain.DomainException;

/** Remitente y asunto de una notificación. */
public record NotificationSettings(String senderName, String senderAddress, String subject) {

  private static final Pattern ADDRESS =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+");

  public NotificationSettings {
    if (senderName == null || senderName.isBlank() || senderName.length() > 120) {
      throw new DomainException("El nombre del remitente es obligatorio (máx. 120)");
    }
    if (senderAddress == null
        || senderAddress.length() > 254
        || !ADDRESS.matcher(senderAddress).matches()) {
      throw new DomainException("Dirección de remitente no válida: " + senderAddress);
    }
    if (subject == null || subject.isBlank() || subject.length() > 200) {
      throw new DomainException("El asunto es obligatorio (máx. 200)");
    }
  }
}
