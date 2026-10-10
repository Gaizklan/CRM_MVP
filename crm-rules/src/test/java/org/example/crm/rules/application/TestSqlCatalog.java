package org.example.crm.rules.application;

import java.util.List;
import java.util.Optional;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.SqlFragment;
import org.example.crm.consumer.application.SegmentationSqlCatalog;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.PreferenceType;

/** Catálogo mínimo para probar la traducción sin base de datos. */
class TestSqlCatalog implements SegmentationSqlCatalog {

  @Override
  public Optional<ProfileColumn> profileColumn(String field) {
    return switch (field) {
      case "registeredAt" -> Optional.of(new ProfileColumn("c.registered_at", true, false));
      case "email" -> Optional.of(new ProfileColumn("c.email_hash", true, false));
      case "phone" -> Optional.of(new ProfileColumn("c.phone_enc", false, false));
      case "status" -> Optional.empty();
      default -> Optional.of(new ProfileColumn("c." + field, true, true));
    };
  }

  @Override
  public Object bindProfileValue(String field, Object value) {
    return field.equals("email") ? "hash(" + value + ")" : value;
  }

  @Override
  public String attributeValueColumn() {
    return "a.value";
  }

  @Override
  public SqlFragment attributeExists(String name, SqlFragment condition) {
    return SqlFragment.template("ATTR(? AND %s)", List.of(name), condition);
  }

  @Override
  public String preferenceTypeColumn() {
    return "p.type";
  }

  @Override
  public String preferenceColumn(PreferenceType type) {
    return "p." + type.name().toLowerCase();
  }

  @Override
  public SqlFragment preferenceExists(String key, SqlFragment condition) {
    return SqlFragment.template("PREF(? AND %s)", List.of(key), condition);
  }

  @Override
  public SqlFragment consentAllowed(ConsentChannel channel) {
    return SqlFragment.of("CONSENT(?)", channel.name());
  }

  @Override
  public SqlFragment subscribedTo(String serviceCode) {
    return SqlFragment.of("SUB(?)", serviceCode);
  }

  @Override
  public SqlFragment selectConsumerIds(BrandId brand, SqlFragment where) {
    return SqlFragment.template("SELECT id WHERE brand = ? AND %s", List.of(brand.value()), where);
  }
}
