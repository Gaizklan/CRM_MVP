package org.example.crm.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

/** Comprueba que las reglas detectan realmente las violaciones (paquete {@code archfixtures}). */
class ArchitectureRulesSelfTest {

  private final JavaClasses fixtures = new ClassFileImporter().importPackages("archfixtures");

  @Test
  void detectsSpringAndJpaInDomain() {
    EvaluationResult result = ArchitectureRules.domainIsPure().evaluate(fixtures);

    assertThat(result.hasViolation()).isTrue();
    assertThat(result.getFailureReport().getDetails())
        .anyMatch(d -> d.contains("ImpureDomainService") && d.contains("springframework"))
        .anyMatch(d -> d.contains("ImpureDomainEntity") && d.contains("jakarta.persistence"));
  }

  @Test
  void detectsAccessToAnotherModulesRepositoryAndEntity() {
    EvaluationResult result =
        ArchitectureRules.noCrossModulePersistenceAccess("archfixtures").evaluate(fixtures);

    assertThat(result.getFailureReport().getDetails())
        .anyMatch(d -> d.contains("módulo 'beta'") && d.contains("AlphaJpaRepository"))
        .anyMatch(d -> d.contains("módulo 'beta'") && d.contains("AlphaEntity"))
        .noneMatch(d -> d.contains("módulo 'alpha' accede"));
  }

  @Test
  void allowsAccessToOwnModulePersistence() {
    JavaClasses alphaOnly = new ClassFileImporter().importPackages("archfixtures.alpha");

    assertThatCode(
            () -> ArchitectureRules.noCrossModulePersistenceAccess("archfixtures").check(alphaOnly))
        .doesNotThrowAnyException();
  }
}
