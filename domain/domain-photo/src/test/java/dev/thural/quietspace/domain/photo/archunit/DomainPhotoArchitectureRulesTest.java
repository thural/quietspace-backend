package dev.thural.quietspace.domain.photo.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainPhotoArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.photo");

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_event_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.event..")
                .allowEmptyShould(true)
                .because("DomainPhotoArchitectureRulesTest must not depend on core.messaging.event");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_user_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.user..")
                .because("DomainPhoto must not depend on domain user (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post..")
                .because("DomainPhoto must not depend on domain post (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .because("DomainPhoto must not depend on domain comment (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction..")
                .because("DomainPhoto must not depend on domain reaction (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_chat_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .because("DomainPhoto must not depend on domain chat (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message..")
                .because("DomainPhoto must not depend on domain message (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .because("DomainPhoto must not depend on domain notification (allowed edges: none (leaf))");
        rule.check(CLASSES);
    }

    @Test
    void has_no_cross_module_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .should().dependOnClassesThat(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .and(JavaClass.Predicates.resideInAPackage("dev.thural.quietspace.domain.."))
                        .and(JavaClass.Predicates.resideOutsideOfPackage("dev.thural.quietspace.domain.photo..")))
                .because("DomainPhoto repositories are module-internal; cross-domain reads go through api query ports");
        rule.check(CLASSES);
    }
}
