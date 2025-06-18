package com.dappstp.dappstp;// Ajusta este paquete a tu estructura

import com.tngtech.archunit.base.DescribedPredicate; // Added import
// Import JavaClass for predicates
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.base.DescribedPredicate.not; // Import not for predicates
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private JavaClasses importedClasses;

    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS) // Opcional: para acelerar si tienes muchos JARS
                .importPackages("com.dappstp.dappstp");
    }

    // --- Reglas existentes (con pequeños ajustes si es necesario) ---
    @Test
    void services_should_be_in_service_package_and_annotated_with_Service() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..") // Usar '..' para flexibilidad
                .and().areNotInterfaces()
                .and().haveSimpleNameNotEndingWith("Dto")
                .and().haveSimpleNameNotEndingWith("Aspect")
                .and().haveSimpleNameNotContaining("Context")
                .and(not(JavaClass.Predicates.ANONYMOUS_CLASSES))
                .and(not(JavaClass.Predicates.INNER_CLASSES))
                .should().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(Component.class) // A veces se usa @Component para servicios también
                .andShould().haveSimpleNameEndingWith("Service").orShould().haveSimpleNameEndingWith("ServiceImpl")
                .as("Las implementaciones de servicios deben estar en un subpaquete 'service', ser clases externas, anotadas con @Service o @Component, terminar con 'Service' o 'ServiceImpl', y no ser DTOs, Aspects o Contexts.");

        rule.check(importedClasses);
    }

    @Test
    void controllers_should_be_in_controller_package_and_annotated() {
        ArchRule rule = classes()
                .that().resideInAPackage("..webservices..") // Usar '..' para flexibilidad
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
                .that().resideInAPackage("..repository..") // Usar '..' para flexibilidad
                .should().beAnnotatedWith(Repository.class)
                .andShould().haveSimpleNameEndingWith("Repository").orShould().haveSimpleNameEndingWith("RepositoryImpl") // Allow Impl suffix
                .as("Los repositorios deben estar en un subpaquete 'repository', anotados con @Repository y terminar con 'Repository' o 'RepositoryImpl'");

        rule.check(importedClasses);
    }

    @Test
    void layered_architecture_dependencies_are_respected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("WebServices").definedBy("..webservices..")
                .layer("Services").definedBy("..service..")
                .layer("Repositories").definedBy("..repository..")
                .layer("Security").definedBy("..security..")
                .layer("Model").definedBy("..model..") // Capa para entidades y objetos de dominio
                .layer("DTOs").definedBy("..dto..")     // Capa para Data Transfer Objects
                .layer("Aspects").definedBy("..aspect..", "..service.scraping.aspect..") // Capa para Aspectos (incluye ApiLoggingAspect y Scraping AOP infra)
                .layer("Config").definedBy("..config..") // Si tienes clases de configuración

                .whereLayer("WebServices").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("WebServices", "Security", "Config")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services", "Aspects") // Permitir que Aspectos accedan a Repositorios
                .whereLayer("Security").mayOnlyBeAccessedByLayers("WebServices", "Services", "Config")
                .whereLayer("Model").mayOnlyBeAccessedByLayers("Repositories", "Services", "DTOs", "WebServices", "Aspects") // Permitir que Aspectos accedan a Modelos
                .whereLayer("DTOs").mayOnlyBeAccessedByLayers("WebServices", "Services")
                .whereLayer("Aspects").mayOnlyBeAccessedByLayers("Services", "WebServices", "Config") // Permitir que Services, WebServices y Config accedan a Aspectos (para anotaciones, configuración, etc.)
                .whereLayer("Config").mayNotBeAccessedByAnyLayer();

        rule.check(importedClasses);
    }

    @Test
    void controllers_should_not_access_repositories_directly() {
        // Esta regla ya está cubierta por la layered_architecture_dependencies_are_respected,
        // pero la mantenemos por su claridad explícita.
        ArchRule rule = noClasses()
                .that().resideInAPackage("..webservices..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");

        rule.check(importedClasses);
    }

    // --- Nuevas Reglas Sugeridas (basadas en tu input) ---

    @Test
    void no_cycles_between_packages() {
        // Detecta ciclos entre los principales paquetes de funcionalidades/capas.
        // Ajusta el patrón "com.dappstp.dappstp.(*).." si tienes una estructura de módulos diferente.
        ArchRule rule = slices().matching("com.dappstp.dappstp.(*)..")
                .should().beFreeOfCycles()
                .as("No debería haber ciclos de dependencia entre los paquetes principales (slices).");
        rule.check(importedClasses);
    }

    @Test
    void classes_named_Foo_should_be_in_foo_package_example() {
        // Ejemplo: si tienes una convención de que clases que empiezan con "Legacy" deben estar en un paquete "legacy"
        // ArchRule rule = classes().that().haveSimpleNameStartingWith("Legacy")
        //        .should().resideInAPackage("..legacy..")
        //        .as("Clases que empiezan con 'Legacy' deben estar en un paquete 'legacy'.");
        // rule.check(importedClasses);
        // Esta es una regla de ejemplo, actívala y ajústala si tienes una convención similar.
    }

    @Test
    void entity_classes_should_follow_conventions() {
        ArchRule rule = classes()
                .that(DescribedPredicate.describe("is annotated with @Entity",
                                (JavaClass javaClass) -> javaClass.isAnnotatedWith(Entity.class))
                        .or(DescribedPredicate.describe("is annotated with @Embeddable",
                                (JavaClass javaClass) -> javaClass.isAnnotatedWith(Embeddable.class)))
                )
                .should().resideInAPackage("..model..")
                // Optionally, if you don't want to enforce suffixes for all model classes:
                // .and(JavaClass.Predicates.simpleNameEndingWith("Entity").or(JavaClass.Predicates.simpleNameEndingWith("Id")))
                .as("Clases anotadas con @Entity o @Embeddable deben estar en el paquete 'model'. Suffixes like 'Entity' or 'Id' are recommended but not strictly enforced by this modified rule.");
        rule.check(importedClasses);
    }

    @Test
    void dto_classes_should_follow_conventions() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Dto").or().haveSimpleNameEndingWith("DTO")
                .should().resideInAPackage("..dto..")
                .as("Clases que terminan con 'Dto' o 'DTO' deben estar en un subpaquete 'dto'.");
        rule.check(importedClasses);
    }

    @Test
    void entityManager_usage_restrictions() {
        // Clases que SON EntityManager (ej. implementaciones custom, menos común)
        // Solo deben ser dependidas por clases en persistencia o servicios (si es necesario para alguna lógica avanzada)
        // y esas clases dependientes deberían ser transaccionales.
        ArchRule implementorsRule = classes()
                .that(JavaClass.Predicates.assignableTo(EntityManager.class))
                .and(not(JavaClass.Predicates.INTERFACES)) // Excluir la interfaz EntityManager misma
                .should().onlyHaveDependentClassesThat()
                .resideInAnyPackage("..repository..", "..service..") // Ajusta si es necesario
                .andShould().onlyHaveDependentClassesThat(
                        DescribedPredicate.describe(
                                "is annotated with @Transactional or has a method annotated with @Transactional",
                                (JavaClass javaClass) -> javaClass.isAnnotatedWith(Transactional.class)
                                        || javaClass.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Transactional.class))
                        )
                )
                .as("Implementaciones de EntityManager solo deben ser dependidas por clases en 'repository' o 'service' que sean transaccionales.");
        // implementorsRule.check(importedClasses); // Descomenta si tienes implementaciones propias de EntityManager

        // Clases que USAN (inyectan/tienen campos de) EntityManager
        // DescribedPredicate<JavaConstructor> constructorHasEntityManagerParameter =
        //     DescribedPredicate.describe("have EntityManager constructor parameter",
        //         (JavaConstructor constructor) -> constructor.getRawParameterTypes().stream()
        //             .anyMatch(paramType -> paramType.isEquivalentTo(EntityManager.class)));

        // DescribedPredicate<JavaMethod> methodHasEntityManagerParameter =
        //     DescribedPredicate.describe("have EntityManager method parameter",
        //         (JavaMethod method) -> method.getRawParameterTypes().stream()
        //             .anyMatch(paramType -> paramType.isEquivalentTo(EntityManager.class)));

        // DescribedPredicate<JavaClass> classUsesEntityManagerInConstructor =
        //     DescribedPredicate.describe("uses EntityManager in constructor",
        //         (JavaClass javaClass) -> javaClass.getConstructors().stream().anyMatch(constructorHasEntityManagerParameter));

        // DescribedPredicate<JavaClass> classUsesEntityManagerInMethod =
        //     DescribedPredicate.describe("uses EntityManager in method",
        //         (JavaClass javaClass) -> javaClass.getMethods().stream().anyMatch(methodHasEntityManagerParameter));
        
        // ArchRule usersRule = classes()
        //          .that(classUsesEntityManagerInConstructor
        //         .or(classUsesEntityManagerInMethod))
        //         .should().resideInAPackage("..repository..") // Principalmente repositorios
        //         .andShould(DescribedPredicate.describe("ser anotadas con @Transactional o tener métodos transaccionales", (JavaClass clazz) ->
        //                 clazz.isAnnotatedWith(Transactional.class) ||
        //                 clazz.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Transactional.class))))
        //         .as("Clases que usan EntityManager directamente (campos, constructores, métodos) deben residir en 'repository' y ser transaccionales.");
        // usersRule.check(importedClasses);
    }

    // --- Reglas generales de codificación (de ArchUnit) ---
    @Test
    void no_classes_should_use_jodatime() {
        NO_CLASSES_SHOULD_USE_JODATIME.check(importedClasses);
    }

    @Test
    void no_classes_should_throw_generic_exceptions() {
        NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(importedClasses);
    }

    @Test
    void no_classes_should_access_standard_streams_directly() {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(importedClasses);
    }

    // --- Reglas sobre Aspectos (requieren que definas cómo identificarlos) ---
    // @Test
    // void aspects_should_follow_conventions_and_be_used_correctly() {
    //     // 1. Convenciones para clases Aspecto
    //     ArchRule aspectDefinitionRule = classes()
    //             .that(DescribedPredicate.describe("is annotated with @Aspect",
    //                     (JavaClass javaClass) -> javaClass.isAnnotatedWith(org.aspectj.lang.annotation.Aspect.class)))
    //             .should().resideInAPackage("..aspect..") // O donde los coloques
    //             .andShould().haveSimpleNameEndingWith("Aspect")
    //             .as("Clases @Aspect deben estar en 'aspect' y terminar con 'Aspect'.");
    //     // aspectDefinitionRule.check(importedClasses);

    //     // 2. Verificar que los servicios de scraping (u otros) son 'aconsejados'
    //     // Esto es más complejo y depende de tus pointcuts o si usas anotaciones custom.
    //     // Ejemplo: si los métodos de scraping deben tener una anotación @LoggableScraping
    //     // ArchRule scrapingMethodsAdvisedRule = methods()
    //     // .that(JavaMember.Predicates.declaredIn(DescribedPredicate.describe("resides in scraping service package",
    //     //        (JavaClass javaClass) -> javaClass.getPackage().getName().contains(".service.scraping"))))
    //     // .and(JavaCodeUnit.Predicates.visibility(JavaModifier.PUBLIC))
    //     // .and(not(JavaCodeUnit.Predicates.constructor()))
    //     // .should(JavaMember.Predicates.annotatedWith(LoggableScraping.class)) // Tu anotación custom
    //     //         .as("Métodos públicos de scraping services deben ser anotados con @LoggableScraping.");
    //     // scrapingMethodsAdvisedRule.check(importedClasses);
    // }


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
