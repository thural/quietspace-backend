package dev.thural.quietspace.domain.comment.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainCommentArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.comment");

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_event_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.event..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on core.messaging.event");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.photo.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.photo.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.photo.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.model..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.photo.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.repository..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.photo.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.reaction.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.reaction.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.reaction.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.model..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.reaction.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.repository..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.reaction.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.chat.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.chat.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.chat.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.model..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.chat.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat.repository..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.chat.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.message.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.message.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.message.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message.model..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.message.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message.repository..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.message.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.adapter..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.notification.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.service..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.notification.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.controller..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.notification.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.model..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.notification.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.repository..")
                .allowEmptyShould(true)
                .because("DomainCommentArchitectureRulesTest must not depend on domain.notification.repository");
        rule.check(CLASSES);
    }
}
