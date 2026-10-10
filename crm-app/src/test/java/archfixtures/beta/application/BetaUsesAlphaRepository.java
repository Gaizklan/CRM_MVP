package archfixtures.beta.application;

import archfixtures.alpha.infrastructure.AlphaEntity;
import archfixtures.alpha.infrastructure.AlphaJpaRepository;

/** Fixture: el módulo beta accede indebidamente a la persistencia del módulo alpha. */
public class BetaUsesAlphaRepository {
  AlphaJpaRepository repository;

  AlphaEntity load(Long id) {
    return repository.findById(id).orElseThrow();
  }
}
