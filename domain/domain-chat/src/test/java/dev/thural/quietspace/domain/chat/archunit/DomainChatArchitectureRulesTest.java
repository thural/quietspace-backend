package dev.thural.quietspace.domain.chat.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class DomainChatArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .withImportOption(new ImportOption.DoNotIncludeArchives())
            .withImportOption(new ImportOption.DoNotIncludeJars())
            .importPackages("dev.thural.quietspace.domain.chat");

    @Test
    void has_no_core_data_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.data..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.data");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_web_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.web..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.web");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_security_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.security..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.security");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_adapter_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.adapter..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.messaging.adapter");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_service_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.service..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.messaging.service");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_controller_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.controller..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.messaging.controller");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_config_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.config..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.messaging.config");
        rule.check(CLASSES);
    }

    @Test
    void has_no_core_messaging_event_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.core.messaging.event..")
                .allowEmptyShould(true)
                .because("DomainChatArchitectureRulesTest must not depend on core.messaging.event");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_post_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.post..")
                .because("DomainChat must not depend on domain post (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_photo_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.photo..")
                .because("DomainChat must not depend on domain photo (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_comment_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.comment..")
                .because("DomainChat must not depend on domain comment (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_reaction_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.reaction..")
                .because("DomainChat must not depend on domain reaction (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_message_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.message..")
                .because("DomainChat must not depend on domain message (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_domain_notification_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat().resideInAPackage("dev.thural.quietspace.domain.notification..")
                .because("DomainChat must not depend on domain notification (allowed edges: domain-user)");
        rule.check(CLASSES);
    }

    @Test
    void has_no_cross_module_repository_dependency() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("dev.thural.quietspace.domain.chat..")
                .should().dependOnClassesThat(JavaClass.Predicates.simpleNameEndingWith("Repository")
                        .and(JavaClass.Predicates.resideInAPackage("dev.thural.quietspace.domain.."))
                        .and(JavaClass.Predicates.resideOutsideOfPackage("dev.thural.quietspace.domain.chat..")))
                .because("DomainChat repositories are module-internal; cross-domain reads go through api query ports");
        rule.check(CLASSES);
    }
}
