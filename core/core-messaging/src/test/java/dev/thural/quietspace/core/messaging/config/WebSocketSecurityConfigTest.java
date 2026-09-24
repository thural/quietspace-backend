package dev.thural.quietspace.core.messaging.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketSecurityConfigTest {

    private final WebSocketSecurityConfig config = new WebSocketSecurityConfig();

    @Test
    void messageAuthorizationManager_shouldBuild() {
        assertThat(config.messageAuthorizationManager()).isNotNull();
    }
}
