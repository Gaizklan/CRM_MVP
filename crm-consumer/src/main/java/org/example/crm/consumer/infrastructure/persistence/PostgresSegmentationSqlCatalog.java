package org.example.crm.consumer.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.crm.common.application.crypto.PiiCipher;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.application.SegmentationSqlCatalog;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.PreferenceType;
import org.springframework.stereotype.Component;

/** Fragmentos SQL (PostgreSQL) sobre las tablas del módulo de consumidores. */
@Component
class PostgresSegmentationSqlCatalog implements SegmentationSqlCatalog {

  /**
   * Email y teléfono están cifrados (P10): el email solo admite igualdad mediante su índice ciego y
   * el teléfono solo {@code exists}.
   */
  private static final Map<String, ProfileColumn> PROFILE =
      Map.of(
          "email", new ProfileColumn("c.email_hash", true, false),
          "phone", new ProfileColumn("c.phone_ciphertext", false, false),
          "language", new ProfileColumn("c.language", true, true),
          "segment", new ProfileColumn("c.segment", true, true),
          "status", new ProfileColumn("c.status", true, true),
          "registeredAt", new ProfileColumn("c.registered_at", true, false));

  private final PiiCipher cipher;

  PostgresSegmentationSqlCatalog(PiiCipher cipher) {
    this.cipher = cipher;
  }

  @Override
  public Object bindProfileValue(String field, Object value) {
    return field.equals("email") ? cipher.blindIndex((String) value) : value;
  }

  @Override
  public Optional<ProfileColumn> profileColumn(String field) {
    return Optional.ofNullable(PROFILE.get(field));
  }

  @Override
  public String attributeValueColumn() {
    return "ca.value";
  }

  @Override
  public SqlFragment attributeExists(String name, SqlFragment condition) {
    return SqlFragment.template(
        "EXISTS (SELECT 1 FROM consumer_attribute ca WHERE ca.consumer_id = c.id AND ca.name = ?"
            + " AND %s)",
        List.of(name), condition);
  }

  @Override
  public String preferenceTypeColumn() {
    return "cp.pref_type";
  }

  @Override
  public String preferenceColumn(PreferenceType type) {
    return switch (type) {
      case STRING -> "cp.string_value";
      case NUMBER -> "cp.number_value";
      case BOOLEAN -> "cp.boolean_value";
      case DATE -> "cp.date_value";
    };
  }

  @Override
  public SqlFragment preferenceExists(String key, SqlFragment condition) {
    return SqlFragment.template(
        "EXISTS (SELECT 1 FROM consumer_preference cp WHERE cp.consumer_id = c.id AND cp.pref_key"
            + " = ? AND %s)",
        List.of(key), condition);
  }

  @Override
  public SqlFragment consentAllowed(ConsentChannel channel) {
    String granted =
        "EXISTS (SELECT 1 FROM consumer_consent cc WHERE cc.consumer_id = c.id AND cc.channel = ?"
            + " AND cc.granted)";
    if (channel == ConsentChannel.CRM_PROGRAM) {
      return SqlFragment.of("(c.status <> 'WITHDRAWN' AND " + granted + ")", channel.name());
    }
    return SqlFragment.of(
        "(c.status <> 'WITHDRAWN' AND " + granted + " AND " + granted + ")",
        ConsentChannel.CRM_PROGRAM.name(),
        channel.name());
  }

  @Override
  public SqlFragment subscribedTo(String serviceCode) {
    return SqlFragment.of(
        "EXISTS (SELECT 1 FROM subscription s JOIN subscription_service ss ON ss.id = s.service_id"
            + " WHERE s.consumer_id = c.id AND s.unsubscribed_at IS NULL AND ss.code = ?)",
        serviceCode);
  }

  @Override
  public SqlFragment selectConsumerIds(BrandId brand, SqlFragment where) {
    return SqlFragment.template(
        "SELECT c.id FROM consumer c WHERE c.brand_id = ? AND %s ORDER BY c.id",
        List.of(brand.value()), where);
  }
}
