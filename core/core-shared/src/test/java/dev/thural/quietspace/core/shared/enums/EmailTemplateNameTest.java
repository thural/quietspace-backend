package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateNameTest {

    @Test
    void activateAccount_hasCorrectTemplateName() {
        assertThat(EmailTemplateName.ACTIVATE_ACCOUNT.getName()).isEqualTo("activate_account");
    }

    @Test
    void values_containsOnlyActivateAccount() {
        assertThat(EmailTemplateName.values()).hasSize(1);
        assertThat(EmailTemplateName.values()[0]).isEqualTo(EmailTemplateName.ACTIVATE_ACCOUNT);
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(EmailTemplateName.valueOf("ACTIVATE_ACCOUNT")).isEqualTo(EmailTemplateName.ACTIVATE_ACCOUNT);
    }
}