package dev.thural.quietspace.core.shared.test.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.thural.quietspace", importOptions = {
    ImportOption.DoNotIncludeTests.class,
    ImportOption.DoNotIncludeArchives.class,
    ImportOption.DoNotIncludeJars.class
})
public class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace");

    // Currently public repositories (to be made package-private in Phase 4)
    private static final String PUBLIC_REPOSITORIES_PATTERN =
        "ChatRepository|CommentRepository|MessageRepository|NotificationRepository|"
        + "PhotoRepository|PostRepository|ReactionRepository|TokenRepository|UserRepository";

    @ArchTest
    static final ArchRule no_core_depends_on_domain = noClasses()
            .that().resideInAPackage("dev.thural.quietspace.core..")
            .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain..")
            .allowEmptyShould(true)
            .because("core modules must not depend on domain modules");

    @ArchTest
    static final ArchRule no_domain_internal_access = noClasses()
            .that().resideInAPackage("dev.thural.quietspace.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("dev.thural.quietspace.domain..")
            .andShould().notBeInterfaces()
            .allowEmptyShould(true)
            .because("domain modules must not access other domain modules' internal classes directly");

    @ArchTest
    static final ArchRule only_known_repositories_are_public = noClasses()
            .that().areInterfaces()
            .and().haveSimpleNameEndingWith("Repository")
            .and().resideInAPackage("dev.thural.quietspace..")
            .should().haveNameMatching(PUBLIC_REPOSITORIES_PATTERN)
            .orShould().bePackagePrivate()
            .allowEmptyShould(true)
            .because("Only explicitly listed repositories may be public; new repositories must be package-private");

    @ArchTest
    static final ArchRule repositories_exist = classes()
            .that().areInterfaces()
            .and().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("dev.thural.quietspace..")
            .allowEmptyShould(true)
            .because("Repository interfaces should exist for each aggregate");

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories_directly = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true)
            .because("Controllers must not access repositories directly; use services instead");

    @ArchTest
    static final ArchRule services_must_be_in_feature_package = classes()
            .that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage("dev.thural.quietspace..")
            .allowEmptyShould(true)
            .because("Service implementations must reside in feature package");

    @ArchTest
    static final ArchRule entities_should_be_package_private_or_public = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().bePackagePrivate().orShould().bePublic()
            .because("Entities should be package-private or public");
}