package ru.hotel.hotelservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "ru.hotel.hotelservice", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String GENERATED_API_PACKAGE = "ru.hotel.hotelapi.api";

    @ArchTest
    static final ArchRule layersDependOnlyDownwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller")
            .definedBy("..controller..")
            .layer("Service")
            .definedBy("..service..")
            .layer("Repository")
            .definedBy("..repository..")
            .whereLayer("Controller")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository")
            .mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule domainDoesNotKnowOuterLayers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("ru.hotel.hotelapi..", "..dto..", "..controller..", "..service..");

    @ArchTest
    static final ArchRule controllersImplementTheGeneratedContract = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .implement(DescribedPredicate.describe(
                    "an interface generated from the OpenAPI contract",
                    (JavaClass type) -> type.getPackageName().equals(GENERATED_API_PACKAGE)));

    @ArchTest
    static final ArchRule controllersDependOnServiceInterfaces = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("ServiceImpl");

    @ArchTest
    static final ArchRule servicesImplementAnInterface = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .haveSimpleNameEndingWith("Impl")
            .andShould(implementAnInterfaceFromTheirPackage());

    @ArchTest
    static final ArchRule noFieldInjection = fields().should().notBeAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule noPackageCycles =
            slices().matching("ru.hotel.hotelservice.(*)..").should().beFreeOfCycles();

    private static ArchCondition<JavaClass> implementAnInterfaceFromTheirPackage() {
        return new ArchCondition<>("implement an interface from their package") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean implementsLocalInterface = item.getRawInterfaces().stream()
                        .anyMatch(type -> type.getPackageName().equals(item.getPackageName()));
                if (!implementsLocalInterface) {
                    events.add(SimpleConditionEvent.violated(
                            item, item.getName() + " does not implement a service interface"));
                }
            }
        };
    }
}
