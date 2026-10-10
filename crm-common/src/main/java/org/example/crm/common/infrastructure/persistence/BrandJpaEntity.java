package org.example.crm.common.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.example.crm.common.domain.Brand;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.BrandStatus;

/** Tabla {@code brand}. No es una entidad con Marca: es el propio catálogo de tenants. */
@Entity
@Table(name = "brand")
public class BrandJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(unique = true)
  private String domain;

  @Column(name = "default_language", nullable = false, length = 10)
  private String defaultLanguage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BrandStatus status;

  protected BrandJpaEntity() {}

  static BrandJpaEntity from(Brand brand) {
    BrandJpaEntity e = new BrandJpaEntity();
    e.id = brand.id().value();
    e.code = brand.code();
    e.name = brand.name();
    e.domain = brand.domain().orElse(null);
    e.defaultLanguage = brand.defaultLanguage();
    e.status = brand.status();
    return e;
  }

  Brand toDomain() {
    return Brand.restore(new BrandId(id), code, name, domain, defaultLanguage, status);
  }
}
