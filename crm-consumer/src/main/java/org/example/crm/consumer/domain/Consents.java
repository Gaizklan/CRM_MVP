package org.example.crm.consumer.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.example.crm.common.domain.BrandId;
import org.example.crm.common.domain.DomainEvent;

/**
 * Consentimientos vigentes de un consumidor por canal.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>Sin registro, un canal <b>no</b> está permitido (opt-in explícito).
 *   <li>Un canal solo está permitido si su consentimiento y el del programa CRM están concedidos.
 *   <li>El opt-out del programa CRM registra además el opt-out de todos los canales concedidos; un
 *       nuevo opt-in al programa no reactiva los canales.
 *   <li>Cada cambio efectivo genera un {@link ConsentChange} auditable y un evento {@link
 *       ConsentChanged}; repetir el estado vigente no genera nada.
 * </ul>
 */
public final class Consents {

  private final BrandId brandId;
  private final ConsumerId consumerId;
  private final Map<ConsentChannel, ConsentStatus> current;
  private final List<ConsentChange> pendingChanges = new ArrayList<>();
  private final List<DomainEvent> pendingEvents = new ArrayList<>();

  private Consents(
      BrandId brandId, ConsumerId consumerId, Map<ConsentChannel, ConsentStatus> current) {
    this.brandId = Objects.requireNonNull(brandId, "brandId");
    this.consumerId = Objects.requireNonNull(consumerId, "consumerId");
    this.current = new EnumMap<>(ConsentChannel.class);
    this.current.putAll(current);
  }

  public static Consents empty(BrandId brandId, ConsumerId consumerId) {
    return new Consents(brandId, consumerId, Map.of());
  }

  public static Consents restore(
      BrandId brandId, ConsumerId consumerId, Map<ConsentChannel, ConsentStatus> current) {
    return new Consents(brandId, consumerId, current);
  }

  /** Concede ({@code granted=true}) o retira el consentimiento de un canal. */
  public void change(
      ConsentChannel channel, boolean granted, ConsentSource source, String actor, Clock clock) {
    Objects.requireNonNull(channel, "channel");
    record(channel, granted, source, actor, clock);
    if (channel.isProgram() && !granted) {
      for (ConsentChannel other : ConsentChannel.values()) {
        if (!other.isProgram()) {
          record(other, false, source, actor, clock);
        }
      }
    }
  }

  private void record(
      ConsentChannel channel, boolean granted, ConsentSource source, String actor, Clock clock) {
    ConsentStatus previous = current.get(channel);
    boolean unchanged = previous == null ? !granted : previous.granted() == granted;
    if (unchanged) {
      return;
    }
    ConsentChange change =
        new ConsentChange(brandId, consumerId, channel, granted, source, actor, clock.instant());
    current.put(channel, new ConsentStatus(granted, source, change.occurredAt()));
    pendingChanges.add(change);
    pendingEvents.add(ConsentChanged.of(change));
  }

  /** Indica si se puede actuar por el canal: opt-in del canal y del programa CRM. */
  public boolean isAllowed(ConsentChannel channel) {
    boolean program = isGranted(ConsentChannel.CRM_PROGRAM);
    return channel.isProgram() ? program : program && isGranted(channel);
  }

  /** Consentimiento registrado del canal (sin considerar el programa). */
  public boolean isGranted(ConsentChannel channel) {
    ConsentStatus status = current.get(channel);
    return status != null && status.granted();
  }

  public Optional<ConsentStatus> status(ConsentChannel channel) {
    return Optional.ofNullable(current.get(channel));
  }

  public Map<ConsentChannel, ConsentStatus> current() {
    return Collections.unmodifiableMap(new EnumMap<>(current));
  }

  public List<ConsentChange> pullChanges() {
    List<ConsentChange> changes = List.copyOf(pendingChanges);
    pendingChanges.clear();
    return changes;
  }

  public List<DomainEvent> pullEvents() {
    List<DomainEvent> events = List.copyOf(pendingEvents);
    pendingEvents.clear();
    return events;
  }

  public BrandId brandId() {
    return brandId;
  }

  public ConsumerId consumerId() {
    return consumerId;
  }
}
