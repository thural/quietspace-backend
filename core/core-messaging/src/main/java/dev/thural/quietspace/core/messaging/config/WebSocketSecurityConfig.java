package dev.thural.quietspace.core.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager() {
        return MessageMatcherDelegatingAuthorizationManager.builder()
                // Protocol frames carry no destination and must never be denied,
                // otherwise STOMP CONNECT is rejected and no session is established.
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT,
                        SimpMessageType.UNSUBSCRIBE, SimpMessageType.DISCONNECT).permitAll()
                .simpDestMatchers("/public/**").permitAll()
                .simpDestMatchers("/app/**").authenticated()
                .simpDestMatchers("/user/**", "/private/**").authenticated()
                .anyMessage().denyAll()
                .build();
    }

    /**
     * Replaces the framework's {@code XorCsrfChannelInterceptor} (picked up by
     * name): message-channel CSRF protection assumes cookie sessions and
     * rejects STOMP CONNECT frames without an echoed handshake token. This
     * application authenticates statelessly via JWT in the CONNECT headers,
     * which browsers cannot attach ambiently, so the check only breaks
     * legitimate clients. HTTP CSRF rules in {@code SecurityConfig} already
     * ignore {@code /ws/**}; destination authorization above still applies.
     */
    @Bean("csrfChannelInterceptor")
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
        };
    }
}
