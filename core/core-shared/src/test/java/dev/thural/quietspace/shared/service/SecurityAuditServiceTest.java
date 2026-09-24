package dev.thural.quietspace.shared.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class SecurityAuditServiceTest {

    private SecurityAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new SecurityAuditService();
    }

    @Test
    void logLoginSuccess_shouldNotThrow() {
        assertThatCode(() -> auditService.logLoginSuccess("user@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void logLoginFailure_shouldNotThrow() {
        assertThatCode(() -> auditService.logLoginFailure("user@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void logLogout_shouldNotThrow() {
        assertThatCode(() -> auditService.logLogout("testuser"))
                .doesNotThrowAnyException();
    }

    @Test
    void logTokenRefresh_shouldNotThrow() {
        assertThatCode(() -> auditService.logTokenRefresh("testuser"))
                .doesNotThrowAnyException();
    }

    @Test
    void logRegistration_shouldNotThrow() {
        assertThatCode(() -> auditService.logRegistration("newuser@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void logAccountActivation_shouldNotThrow() {
        assertThatCode(() -> auditService.logAccountActivation("user@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void logAccessDenied_shouldNotThrow() {
        assertThatCode(() -> auditService.logAccessDenied("/api/v1/admin", "testuser"))
                .doesNotThrowAnyException();
    }

    @Test
    void logEvent_shouldNotThrow() {
        assertThatCode(() -> auditService.logEvent("CUSTOM_EVENT", "testuser", "some detail"))
                .doesNotThrowAnyException();
    }

    @Test
    void logEvent_withNullPrincipal_shouldNotThrow() {
        assertThatCode(() -> auditService.logEvent("CUSTOM_EVENT", null, "detail"))
                .doesNotThrowAnyException();
    }

    @Test
    void logEvent_withNullEmail_shouldNotThrow() {
        assertThatCode(() -> auditService.logEvent("TEST", "not-an-email", "detail"))
                .doesNotThrowAnyException();
    }
}
