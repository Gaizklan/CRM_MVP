package archfixtures.alpha.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Fixture: dominio que incumple la regla al depender de JPA. */
@Entity
public class ImpureDomainEntity {
  @Id Long id;
}
