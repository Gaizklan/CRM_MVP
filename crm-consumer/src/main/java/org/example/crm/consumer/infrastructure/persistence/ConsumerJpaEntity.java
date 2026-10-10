package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.example.crm.common.application.crypto.PiiCipher;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;
import org.example.crm.consumer.domain.Consumer;
import org.example.crm.consumer.domain.ConsumerId;
import org.example.crm.consumer.domain.ConsumerStatus;
import org.example.crm.consumer.domain.Email;
import org.example.crm.consumer.domain.LanguageTag;
import org.example.crm.consumer.domain.PhoneNumber;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "consumer")
public class ConsumerJpaEntity extends BrandScopedEntity {

  @Id private UUID id;

  /** Email cifrado (AES-GCM, ver ADR 0002). */
  @Column(name = "email_ciphertext", nullable = false, length = 512)
  private String emailCiphertext;

  /** Índice ciego (HMAC) del email normalizado: búsqueda y unicidad por Marca. */
  @Column(name = "email_hash", nullable = false, length = 64)
  private String emailHash;

  /** Teléfono cifrado (AES-GCM). */
  @Column(name = "phone_ciphertext", length = 512)
  private String phoneCiphertext;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(length = 32)
  private String segment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ConsumerStatus status;

  @Column(name = "registered_at", nullable = false, updatable = false)
  private Instant registeredAt;

  @ElementCollection
  @CollectionTable(name = "consumer_attribute", joinColumns = @JoinColumn(name = "consumer_id"))
  @MapKeyColumn(name = "name", length = 64)
  @Column(name = "value", nullable = false, length = 1000)
  @BatchSize(size = 100)
  private Map<String, String> attributes = new HashMap<>();

  @ElementCollection
  @CollectionTable(name = "consumer_preference", joinColumns = @JoinColumn(name = "consumer_id"))
  @MapKeyColumn(name = "pref_key", length = 64)
  @BatchSize(size = 100)
  private Map<String, PreferenceEmbeddable> preferences = new HashMap<>();

  protected ConsumerJpaEntity() {}

  static ConsumerJpaEntity create(Consumer consumer, PiiCipher cipher) {
    ConsumerJpaEntity e = new ConsumerJpaEntity();
    e.id = consumer.id().value();
    e.registeredAt = consumer.registeredAt();
    e.assignBrand(consumer.brandId());
    e.apply(consumer, cipher);
    return e;
  }

  void apply(Consumer consumer, PiiCipher cipher) {
    String hash = cipher.blindIndex(consumer.email().value());
    if (!hash.equals(emailHash) || cipher.needsReencryption(emailCiphertext)) {
      emailCiphertext = cipher.encrypt(consumer.email().value());
      emailHash = hash;
    }
    String phone = consumer.phone().map(PhoneNumber::value).orElse(null);
    String currentPhone = phoneCiphertext == null ? null : cipher.decrypt(phoneCiphertext);
    if (!java.util.Objects.equals(phone, currentPhone)
        || cipher.needsReencryption(phoneCiphertext)) {
      phoneCiphertext = cipher.encrypt(phone);
    }
    language = consumer.language().value();
    segment = consumer.segment().orElse(null);
    status = consumer.status();
    attributes.clear();
    attributes.putAll(consumer.attributes());
    preferences.clear();
    consumer.preferences().forEach((k, v) -> preferences.put(k, PreferenceEmbeddable.from(v)));
  }

  Consumer toDomain(PiiCipher cipher) {
    return Consumer.restore(
        new ConsumerId(id),
        getBrandId(),
        new Email(cipher.decrypt(emailCiphertext)),
        phoneCiphertext == null ? null : new PhoneNumber(cipher.decrypt(phoneCiphertext)),
        new LanguageTag(language),
        segment,
        status,
        registeredAt,
        attributes,
        preferences.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toDomain())));
  }

  UUID getId() {
    return id;
  }
}
