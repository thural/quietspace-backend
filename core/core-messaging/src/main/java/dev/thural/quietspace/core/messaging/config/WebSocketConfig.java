package dev.thural.quietspace.websocket.config;

import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final CustomHandshakeHandler handshakeHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user", "/public", "/private");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler)
                .withSockJS();

        registry.addEndpoint("/ws/raw")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(handshakeHandler);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ws-inbound-");
        executor.setTaskDecorator(task -> {
            SecurityContext context = SecurityContextHolder.getContext();
            SecurityContext snapshot = SecurityContextHolder.createEmptyContext();
            snapshot.setAuthentication(context.getAuthentication());
            return () -> {
                SecurityContextHolder.setContext(snapshot);
                try {
                    task.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
        });
        executor.initialize();
        registration.taskExecutor(executor);
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
                if (message == null) return null;
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    handleConnect(accessor);
                } else if (accessor.getCommand() == StompCommand.SEND) {
                    log.warn("SEND dest={} user={}", accessor.getDestination(),
                            accessor.getUser() != null ? accessor.getUser().getName() : "null");
                    restoreSecurityContext(accessor);
                } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
                    log.warn("SUBSCRIBE dest={} user={}", accessor.getDestination(),
                            accessor.getUser() != null ? accessor.getUser().getName() : "null");
                    restoreSecurityContext(accessor);
                }

                return message;
            }

            private void handleConnect(StompHeaderAccessor accessor) {
                String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                    log.warn("Missing or invalid Authorization header in STOMP CONNECT");
                    return;
                }

                try {
                    String token = authorizationHeader.substring(7);
                    String username = jwtService.extractUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    User user = (User) userDetails;

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, userDetails.getPassword(), userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    UsernamePasswordAuthenticationToken stompToken = new UsernamePasswordAuthenticationToken(
                            user.getId().toString(), userDetails.getPassword(), userDetails.getAuthorities()
                    );
                    accessor.setUser(stompToken);
                    log.warn("STOMP CONNECT auth: SUCCESS principal={}", user.getId());
                } catch (Exception e) {
                    log.warn("Authentication failed in STOMP CONNECT: {}; cause: {}", e.getMessage(), e.getClass().getSimpleName());
                }
            }

            private boolean restoreSecurityContext(StompHeaderAccessor accessor) {
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    return true;
                }

                Principal principal = accessor.getUser();
                if (principal == null || principal.getName() == null) {
                    return false;
                }

                try {
                    UUID userId = UUID.fromString(principal.getName());
                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null) {
                        log.warn("User not found for principal={}", principal.getName());
                        return false;
                    }
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    return true;
                } catch (Exception e) {
                    log.warn("Failed to restore auth for principal={}: {}", principal.getName(), e.getMessage());
                    return false;
                }
            }
        });
    }


}
