package dev.thural.quietspace.domain.post.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainPostArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.post");

    @Test
    void has_no_core_data_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.data..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.data");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_event_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.event..")
                .allowEmptyShould(true)
                .because("DomainPostArchitectureRulesTest must not depend on core.messaging.event");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .because("DomainPost must not depend on domain chat (allowed edges: domain-user, domain-photo, domain-reaction, domain-comment, domain-notification)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message..")
                .because("DomainPost must not depend on domain message (allowed edges: domain-user, domain-photo, domain-reaction, domain-comment, domain-notification)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_cross_module_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.post..")
                .should().dependOnClassesThat(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .and(JavaClass.Predicates.resideInAPackage("dev.thural.quietspace.domain.."))
                        .and(JavaClass.Predicates.resideOutsideOfPackage("dev.thural.quietspace.domain.post..")))
                .because("DomainPost repositories are module-internal; cross-domain reads go through api query ports");
        rule.check(CLASSES);
    }
}
