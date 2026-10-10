package org.example.crm.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.example.crm.common.infrastructure.persistence.BrandScopedEntity;
import org.example.crm.consumer.domain.ConsentChannel;
import org.example.crm.consumer.domain.ConsentSource;
import org.example.crm.consumer.domain.ConsentStatus;

/** Estado vigente del consentimiento de un consumidor en un canal. */
@Entity
@Table(name = "consumer_consent")
public class ConsentJpaEntity extends BrandScopedEntity {

  @EmbeddedId private Key key;

  @Column(nullable = false)
  private boolean granted;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ConsentSource source;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  protected ConsentJpaEntity() {}

  ConsentJpaEntity(UUID consumerId, ConsentChannel channel) {
    this.key = new Key(consumerId, channel);
  }

  void apply(ConsentStatus status) {
    granted = status.granted();
    source = status.source();
    changedAt = status.since();
  }

  ConsentChannel channel() {
    return key.channel;
  }

  ConsentStatus toStatus() {
    return new ConsentStatus(granted, source, changedAt);
  }

  @Embeddable
  public static class Key implements Serializable {
    @Column(name = "consumer_id")
    private UUID consumerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ConsentChannel channel;

    protected Key() {}

    Key(UUID consumerId, ConsentChannel channel) {
      this.consumerId = consumerId;
      this.channel = channel;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Key k && consumerId.equals(k.consumerId) && channel == k.channel;
    }

    @Override
    public int hashCode() {
      return consumerId.hashCode() * 31 + channel.hashCode();
    }
  }
}
