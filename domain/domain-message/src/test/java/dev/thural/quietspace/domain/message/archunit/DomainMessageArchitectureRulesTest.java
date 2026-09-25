package dev.thural.quietspace.domain.message.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainMessageArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.message");

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.post.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.post.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.post.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post.model..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.post.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post.repository..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.post.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.photo.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.photo.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.photo.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.model..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.photo.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo.repository..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.photo.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.comment.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.comment.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.comment.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.model..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.comment.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment.repository..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.comment.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.reaction.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.reaction.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.reaction.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.model..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.reaction.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction.repository..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.reaction.repository");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.adapter..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.notification.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.service..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.notification.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.controller..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.notification.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_model_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.model..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.notification.model");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.message..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification.repository..")
                .allowEmptyShould(true)
                .because("DomainMessageArchitectureRulesTest must not depend on domain.notification.repository");
        rule.check(CLASSES);
    }
}
