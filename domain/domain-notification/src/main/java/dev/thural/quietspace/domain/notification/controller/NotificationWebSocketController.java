package dev.thural.quietspace.domain.notification.controller;

import dev.thural.quietspace.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.NOTIFICATION_SEEN;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationWebSocketController {

    private final NotificationService notificationService;

    @MessageMapping(NOTIFICATION_SEEN)
    void markMessageSeen(@DestinationVariable UUID notificationId) {
        notificationService.handleSeen(notificationId);
    }

}
