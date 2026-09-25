package dev.thural.quietspace.domain.notification.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainNotificationArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.notification");

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.adapter..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.photo.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.service..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.photo.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.controller..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.photo.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.model..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.photo.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.repository..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.photo.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.adapter..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.comment.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.service..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.comment.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.controller..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.comment.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.model..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.comment.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.repository..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.comment.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.adapter..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.reaction.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.service..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.reaction.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.controller..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.reaction.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.model..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.reaction.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.repository..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.reaction.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.adapter..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.chat.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.service..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.chat.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.controller..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.chat.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.model..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.chat.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.repository..")
                .allowEmptyShould(true)
                .because("DomainNotificationArchitectureRulesTest must not depend on domain.chat.repository");
        rule.check(CLASSES);
    }
}
