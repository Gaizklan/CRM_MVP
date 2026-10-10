package archfixtures.shared.infrastructure;

import jakarta.persistence.MappedSuperclass;

/** Fixture: base de persistencia del kernel compartido, utilizable desde cualquier módulo. */
@MappedSuperclass
public abstract class SharedBaseEntity {}
