package org.example.crm.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reglas de arquitectura de la plataforma (brief 01). Parametrizadas por paquete raíz para poder
 * probarlas contra paquetes de ejemplo que las incumplen.
 */
final class ArchitectureRules {

  private ArchitectureRules() {}

  /**
   * {@code domain} es puro: sin Spring, JPA/Hibernate, ni capas {@code application}/{@code
   * infrastructure}.
   */
  static ArchRule domainIsPure() {
    return noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "javax.persistence..",
            "org.hibernate..",
            "..infrastructure..",
            "..application..")
        .as("las clases de dominio no dependen de Spring, JPA ni de application/infrastructure")
        .allowEmptyShould(true);
  }

  /**
   * Ningún módulo accede a entidades JPA, repositorios Spring Data ni a la capa de infraestructura
   * de otro módulo. Los módulos se comunican por puertos (application) o eventos de dominio. El
   * kernel compartido ({@code crm-common}) es la excepción: aporta la base de persistencia común.
   */
  static ArchRule noCrossModulePersistenceAccess(String rootPackage, String sharedKernelModule) {
    return classes()
        .that()
        .resideInAPackage(rootPackage + "..")
        .should(notAccessPersistenceOfOtherModules(rootPackage, sharedKernelModule))
        .as(
            "ningún módulo accede a repositorios/entidades JPA ni a la infraestructura de otro"
                + " módulo")
        .allowEmptyShould(true);
  }

  /**
   * @param sharedKernelModule módulo kernel compartido ({@code common}): su infraestructura base
   *     (p. ej. {@code BrandScopedEntity}) puede usarse desde cualquier módulo.
   */
  private static ArchCondition<JavaClass> notAccessPersistenceOfOtherModules(
      String rootPackage, String sharedKernelModule) {
    Pattern modulePattern =
        Pattern.compile("^" + Pattern.quote(rootPackage) + "\\.([^.]+)(\\..*)?$");
    return new ArchCondition<>("not access persistence of other modules") {
      @Override
      public void check(JavaClass origin, ConditionEvents events) {
        Optional<String> originModule = moduleOf(origin, modulePattern);
        for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
          JavaClass target = dependency.getTargetClass();
          Optional<String> targetModule = moduleOf(target, modulePattern);
          if (targetModule.isEmpty()
              || targetModule.equals(originModule)
              || originModule.isEmpty()
              || targetModule.get().equals(sharedKernelModule)) {
            continue;
          }
          if (isPersistenceType(target) || target.getPackageName().contains(".infrastructure")) {
            events.add(
                SimpleConditionEvent.violated(
                    dependency,
                    "módulo '%s' accede a %s del módulo '%s': %s"
                        .formatted(
                            originModule.get(),
                            target.getName(),
                            targetModule.get(),
                            dependency.getDescription())));
          }
        }
      }
    };
  }

  private static Optional<String> moduleOf(JavaClass javaClass, Pattern modulePattern) {
    Matcher matcher = modulePattern.matcher(javaClass.getPackageName());
    return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
  }

  private static boolean isPersistenceType(JavaClass target) {
    return target.isAnnotatedWith("jakarta.persistence.Entity")
        || target.isAnnotatedWith("jakarta.persistence.Embeddable")
        || target.isAnnotatedWith("jakarta.persistence.MappedSuperclass")
        || target.isAssignableTo("org.springframework.data.repository.Repository");
  }
}
