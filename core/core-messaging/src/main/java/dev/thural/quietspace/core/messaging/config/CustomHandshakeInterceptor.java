package dev.thural.quietspace.websocket.config;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(@Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response, @Nullable WebSocketHandler wsHandler, @Nullable Map<String, Object> attributes) throws Exception {
        if (attributes == null) return false;
        SecurityContext context = SecurityContextHolder.getContext();
        attributes.put("SPRING_SECURITY_CONTEXT", context);
        return true;
    }

    @Override
    public void afterHandshake(@Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response, @Nullable WebSocketHandler wsHandler, @Nullable Exception exception) {
        // do something
    }
}
