package dev.thural.quietspace.core.messaging.config;

import dev.thural.quietspace.core.shared.ports.WebSocketUserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final WebSocketUserPort userPort;

    @Override
    protected Principal determineUser(
            @Nullable ServerHttpRequest request,
            @Nullable WebSocketHandler wsHandler,
            @Nullable Map<String, Object> attributes
    ) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("No authentication available during WebSocket handshake; deferring to STOMP-level auth");
            return null;
        }
        try {
            UUID userId = userPort.currentUserId();
            log.info("user id at CustomHandshakeHandler: {}", userId);
            return new StompPrincipal(userId.toString());
        } catch (Exception e) {
            log.warn("Could not determine user during WebSocket handshake: {}", e.getMessage());
            return null;
        }
    }

}
