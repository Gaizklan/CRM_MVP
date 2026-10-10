package org.example.crm.app.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.example.crm.common.application.crypto.PiiCipher;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

/**
 * P10 (2/3): cifra el email y el teléfono existentes con la clave activa y calcula el índice ciego.
 * Es una migración Java porque necesita la clave externa (nunca disponible en SQL versionado).
 */
@Component
public class V8__EncryptConsumerPii extends BaseJavaMigration {

  private final PiiCipher cipher;

  public V8__EncryptConsumerPii(PiiCipher cipher) {
    this.cipher = cipher;
  }

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    try (PreparedStatement select =
            connection.prepareStatement("SELECT id, email, phone FROM consumer");
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE consumer SET email_ciphertext = ?, email_hash = ?, phone_ciphertext = ?"
                    + " WHERE id = ?");
        ResultSet rows = select.executeQuery()) {
      while (rows.next()) {
        String email = rows.getString("email");
        String phone = rows.getString("phone");
        update.setString(1, cipher.encrypt(email));
        update.setString(2, cipher.blindIndex(email));
        update.setString(3, cipher.encrypt(phone));
        update.setObject(4, rows.getObject("id", UUID.class));
        update.addBatch();
      }
      update.executeBatch();
    }
  }
}
