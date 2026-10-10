package org.example.crm.common.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.crm.common.domain.DomainEvent;

/**
 * Serialización JSON de eventos de dominio para el outbox; solo admite tipos {@link DomainEvent}.
 */
public class OutboxEventSerializer {

  private final ObjectMapper mapper;

  public OutboxEventSerializer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String typeOf(DomainEvent event) {
    return event.getClass().getName();
  }

  public String serialize(DomainEvent event) {
    try {
      return mapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo serializar el evento " + typeOf(event), e);
    }
  }

  public DomainEvent deserialize(String type, String payload) {
    Class<?> clazz;
    try {
      clazz = Class.forName(type, false, getClass().getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Tipo de evento desconocido: " + type, e);
    }
    if (!DomainEvent.class.isAssignableFrom(clazz) || clazz.isInterface()) {
      throw new IllegalStateException("El tipo " + type + " no es un evento de dominio");
    }
    try {
      return (DomainEvent) mapper.readValue(payload, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo deserializar el evento " + type, e);
    }
  }
}
