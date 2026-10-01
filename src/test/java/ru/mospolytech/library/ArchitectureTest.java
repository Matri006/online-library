package ru.mospolytech.library;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(
        packages = "ru.mospolytech.library",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule jdbc_is_confined_to_repositories =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.jdbc..", "javax.sql..")
                    .because("database access belongs to repositories");

    @ArchTest
    static final ArchRule repositories_are_only_used_by_services =
            noClasses()
                    .that()
                    .resideOutsideOfPackages("..services..", "..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_upper_layers =
            noClasses()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..services..", "..controllers..", "..security..", "..configs..");

    @ArchTest
    static final ArchRule repositories_do_not_consume_http_requests =
            noClasses()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .haveNameMatching(".*\\.dto\\.Requests(\\$.*)?");

    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers =
            noClasses()
                    .that()
                    .resideInAPackage("..services..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..controllers..", "jakarta.servlet..", "org.springframework.web..");

    @ArchTest
    static final ArchRule entities_are_independent =
            noClasses()
                    .that()
                    .resideInAPackage("..entities..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..services..", "..repository..", "..controllers..", "..dto..");

    @ArchTest
    static final ArchRule controllers_do_not_manage_transactions =
            noClasses()
                    .that()
                    .resideInAPackage("..controllers..")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName(Transactional.class.getName());
}
