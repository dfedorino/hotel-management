package ru.hotel.bookingservice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "ru.hotel.bookingservice", importOptions = ImportOption.DoNotIncludeTests.class)
// ArchUnit runs @ArchTest methods and injects its concrete JavaClasses type.
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.LooseCoupling"})
class ArchitectureTest {

    private static final String GENERATED_API_PACKAGE = "ru.hotel.bookingapi.api";

    @ArchTest
    void layersDependOnlyDownwards(JavaClasses classes) {
        layeredArchitecture()
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
                .mayOnlyBeAccessedByLayers("Service")
                .check(classes);
    }

    @ArchTest
    void domainDoesNotKnowOuterLayers(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("ru.hotel.bookingapi..", "ru.hotel.hotelapi..", "..controller..", "..service..")
                .check(classes);
    }

    @ArchTest
    void controllersImplementTheGeneratedContract(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .implement(DescribedPredicate.describe(
                        "an interface generated from the OpenAPI contract",
                        (JavaClass type) -> GENERATED_API_PACKAGE.equals(type.getPackageName())))
                .check(classes);
    }

    @ArchTest
    void controllersDependOnServiceInterfaces(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..controller..")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("ServiceImpl")
                .check(classes);
    }

    @ArchTest
    void servicesImplementAnInterface(JavaClasses classes) {
        classes()
                .that()
                .areAnnotatedWith(Service.class)
                .should()
                .haveSimpleNameEndingWith("Impl")
                .andShould(implementAnInterfaceFromTheirPackage())
                .check(classes);
    }

    @ArchTest
    void sagaSeesHotelServiceOnlyThroughItsPort(JavaClasses classes) {
        noClasses()
                .that()
                .resideInAPackage("..service..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..client..", "ru.hotel.hotelapi.api..", "feign..")
                .check(classes);
    }

    @ArchTest
    void noFieldInjection(JavaClasses classes) {
        fields().should().notBeAnnotatedWith(Autowired.class).check(classes);
    }

    @ArchTest
    void noPackageCycles(JavaClasses classes) {
        slices().matching("ru.hotel.bookingservice.(*)..")
                .should()
                .beFreeOfCycles()
                .check(classes);
    }

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
