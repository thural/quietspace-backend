package dev.thural.quietspace.websocket.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
class WebSocketSecurityConfigTest {

    @Autowired(required = false)
    private WebSocketSecurityConfig webSocketSecurityConfig;

    @Test
    void config_shouldLoad() {
        assertThat(webSocketSecurityConfig).isNotNull();
    }
}
