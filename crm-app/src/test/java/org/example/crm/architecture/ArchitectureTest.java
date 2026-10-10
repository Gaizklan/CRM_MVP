package org.example.crm.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Verifica la arquitectura hexagonal de todos los módulos de producción. */
class ArchitectureTest {

  private static final String ROOT = "org.example.crm";
  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @Test
  void domainHasNoFrameworkOrOuterLayerDependencies() {
    ArchitectureRules.domainIsPure().check(productionClasses);
  }

  @Test
  void modulesDoNotAccessPersistenceOfOtherModules() {
    ArchitectureRules.noCrossModulePersistenceAccess(ROOT, "common").check(productionClasses);
  }
}
