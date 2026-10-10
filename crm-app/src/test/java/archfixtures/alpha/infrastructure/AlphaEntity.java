package archfixtures.alpha.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Fixture: entidad JPA del módulo alpha. */
@Entity
public class AlphaEntity {
  @Id Long id;
}
