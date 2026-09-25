package dev.thural.quietspace.domain.user.audit;

import dev.thural.quietspace.domain.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationAuditAwareTest {

    private final ApplicationAuditAware auditAware = new ApplicationAuditAware();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_givenNoAuthentication_shouldBeEmpty() {
        SecurityContextHolder.clearContext();

        assertThat(auditAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void getCurrentAuditor_givenAnonymous_shouldBeEmpty() {
        var anonymous = new AnonymousAuthenticationToken("key", "anon",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThat(auditAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void getCurrentAuditor_givenUserPrincipal_shouldReturnUserId() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("u").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThat(auditAware.getCurrentAuditor()).hasValue(id.toString());
    }

    @Test
    void getCurrentAuditor_givenNonUserPrincipal_shouldBeEmpty() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-username", null, List.of()));

        assertThat(auditAware.getCurrentAuditor()).isEmpty();
    }
}
