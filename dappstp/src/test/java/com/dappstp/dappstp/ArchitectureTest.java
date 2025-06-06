package com.dappstp.dappstp;// Ajusta este paquete a tu estructura

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaClass; // Import JavaClass for predicates
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import com.tngtech.archunit.base.DescribedPredicate; // Added import
import static com.tngtech.archunit.base.DescribedPredicate.not; // Import not for predicates

// import static com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction.haveSimpleNameEndingWith;

class ArchitectureTest {

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS) // Opcional: para acelerar si tienes muchos JARS
                .importPackages("com.dappstp.dappstp");
    }

    @Test
    void services_should_be_in_service_package_and_annotated_with_Service() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.dappstp.dappstp.service..") // More specific package
                .and().areNotInterfaces() // Focus on concrete classes, interfaces might not be annotated
                .and().haveSimpleNameNotEndingWith("Dto") // Exclude DTOs - Corrected
                .and().haveSimpleNameNotEndingWith("Aspect") // Exclude Aspects - Corrected
                .and().haveSimpleNameNotContaining("Context") // Exclude Context related classes - Corrected
                .and(not(DescribedPredicate.describe("is an anonymous class", JavaClass::isAnonymousClass)))
                .and(not(DescribedPredicate.describe("is an inner class", JavaClass::isInnerClass)))
                .should().beAnnotatedWith(Service.class)
                .andShould().haveSimpleNameEndingWith("Service").orShould().haveSimpleNameEndingWith("ServiceImpl")
                .as("Las implementaciones de servicios deben estar en el paquete 'com.dappstp.dappstp.service..', ser clases externas, anotadas con @Service, terminar con 'Service' o 'ServiceImpl', y no ser DTOs, Aspects o Contexts.");

        rule.check(importedClasses);
    }

    @Test
    void controllers_should_be_in_controller_package_and_annotated() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.dappstp.dappstp.webservices..") // Corrected package for controllers
                .and().resideOutsideOfPackage("..dto..") // Exclude DTO subpackages - Corrected
                .should().beAnnotatedWith(RestController.class) // O Controller.class si usas vistas de servidor
                .orShould().beAnnotatedWith(Controller.class)
                .andShould().haveSimpleNameEndingWith("Controller")
                .as("Los controladores deben estar en el paquete 'com.dappstp.dappstp.webservices..', anotados con @RestController o @Controller y terminar con 'Controller'");

        rule.check(importedClasses);
    }

    @Test
    void repositories_should_be_in_repository_package_and_annotated_with_Repository() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.dappstp.dappstp.repository..") // More specific package
                .should().beAnnotatedWith(Repository.class)
                .andShould().haveSimpleNameEndingWith("Repository").orShould().haveSimpleNameEndingWith("RepositoryImpl") // Allow Impl suffix
                .as("Los repositorios deben estar en el paquete 'repository' y preferiblemente anotados con @Repository o terminar con 'Repository'");

        rule.check(importedClasses);
    }

    @Test
    void layered_architecture_dependencies_are_respected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controllers").definedBy("com.dappstp.dappstp.webservices..") // Corrected package
                .layer("Services").definedBy("com.dappstp.dappstp.service..")     // More specific package
                .layer("Repositories").definedBy("com.dappstp.dappstp.repository..") // More specific package
                .layer("Security").definedBy("com.dappstp.dappstp.security..")       // Added Security layer
                //.layer("Domain").definedBy("..domain..") // Si tienes una capa de dominio explícita

                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Security")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
                .whereLayer("Security").mayOnlyBeAccessedByLayers("Controllers"); // Allow Controllers to access Security (e.g. for JwtToken)

        rule.check(importedClasses);
    }

    @Test
    void controllers_should_not_access_repositories_directly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.dappstp.dappstp.webservices..")
                .should().dependOnClassesThat().resideInAPackage("com.dappstp.dappstp.repository..");

        rule.check(importedClasses);
    }

    
//    @Test
//    void services_should_not_contain_anonymous_inner_classes_violating_rules() {
//        // This is a bit harder to enforce generically for naming if they are true helper anonymous classes.
//        // For now, we rely on the main service rule being more specific.
//        // If PredictionService$1 is a problem, it might indicate a refactoring opportunity in PredictionService.
//        // Consider if that anonymous class should be a private static nested class or a separate class.
//        ArchRule rule = classes()
//                .that(DescribedPredicate.describe("has a simple name containing '$'", (JavaClass javaClass) -> javaClass.getSimpleName().contains("$")))
//                .and().resideInAPackage("com.dappstp.dappstp.service..") // Corrected package
//                .should().notBeAnnotatedWith(Service.class) // Inner classes usually aren't services themselves
//                .as("Inner/Anonymous classes dentro de paquetes de servicio no deberían estar anotadas con @Service ni seguir convenciones de nomenclatura de servicios principales.");
//        rule.allowEmptyShould(true); // Allow this rule to pass if no such classes are found
//        // This rule is more of a check/awareness. You might need to refine it or allow specific exceptions.
//        // If it still "fails to check any classes", it means no such classes were found by the importer in the specified packages.
//        rule.check(importedClasses);
//    }
}
