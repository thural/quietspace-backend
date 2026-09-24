package dev.thural.quietspace.websocket.event.listener;

import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.websocket.event.message.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static dev.thural.quietspace.websocket.event.EventType.CONNECT;
import static dev.thural.quietspace.websocket.event.EventType.DISCONNECT;
import static dev.thural.quietspace.shared.enums.StatusType.OFFLINE;
import static dev.thural.quietspace.shared.enums.StatusType.ONLINE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketEventListener {

    private final SimpMessageSendingOperations messageTemplate;
    private final UserService userService;

    String extractUsernameFromSocketEvent(AbstractSubProtocolEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headers.getUser();
        if (user == null) return null;
        return user.getName();
    }

    @EventListener
    void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        String username = extractUsernameFromSocketEvent(event);
        log.info("user has disconnected with username: {}", username);

        userService.setOnlineStatus(username, OFFLINE);
        BaseEvent payload = BaseEvent.builder()
                .message(username).type(DISCONNECT).build();

        // TODO: send only to followings instead of public
        messageTemplate.convertAndSend("/public", payload);
    }

    @EventListener
    void handleWebSocketConnect(SessionConnectEvent event) {
        String username = extractUsernameFromSocketEvent(event);
        log.info("user has connected with username: {}", username);

        userService.setOnlineStatus(username, ONLINE);
        BaseEvent payload = BaseEvent.builder()
                .message(username).type(CONNECT).build();

        log.info("user has connected with username: {}", username);

        // TODO: send only to followings instead of public
        messageTemplate.convertAndSend("/public", payload);
    }

}
