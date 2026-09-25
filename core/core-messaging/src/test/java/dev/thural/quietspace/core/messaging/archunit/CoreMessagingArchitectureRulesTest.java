package dev.thural.quietspace.core.messaging.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreMessagingArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.core.messaging");

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.core.messaging..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("CoreMessagingArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }
}
