package archfixtures.alpha.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

/** Fixture: repositorio del módulo alpha. */
public interface AlphaJpaRepository extends JpaRepository<AlphaEntity, Long> {}
