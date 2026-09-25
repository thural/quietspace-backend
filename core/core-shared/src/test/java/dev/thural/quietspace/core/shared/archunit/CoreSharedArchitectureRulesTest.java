package dev.thural.quietspace.core.shared.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreSharedArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.core.shared");

    @Test
    void has_no_core_data_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.core.shared..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.data..")
                .allowEmptyShould(true)
                .because("CoreSharedArchitectureRulesTest must not depend on core.data");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.core.shared..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("CoreSharedArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.core.shared..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("CoreSharedArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.core.shared..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging..")
                .allowEmptyShould(true)
                .because("CoreSharedArchitectureRulesTest must not depend on core.messaging");
        rule.check(CLASSES);
    }
}
