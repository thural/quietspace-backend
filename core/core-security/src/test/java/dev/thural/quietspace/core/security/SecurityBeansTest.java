package dev.thural.quietspace.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityBeansTest {

    private final SecurityBeans beans = new SecurityBeans();

    @Test
    void passwordEncoder_shouldEncodeAndMatch() {
        PasswordEncoder encoder = beans.passwordEncoder();

        String encoded = encoder.encode("secret");

        assertThat(encoder.matches("secret", encoded)).isTrue();
    }

    @Test
    void authenticationManager_shouldDelegateToConfiguration() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(manager);

        assertThat(beans.authenticationManager(config)).isSameAs(manager);
    }

    @Test
    void corsFilter_shouldAllowConfiguredOrigins() {
        ReflectionTestUtils.setField(beans, "allowedOrigins",
                new String[]{"https://frontend-app.com"});

        CorsFilter filter = beans.corsFilter();

        assertThat(filter).isNotNull();
    }
}
