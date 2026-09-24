package dev.thural.quietspace.websocket.controller;

import dev.thural.quietspace.websocket.event.message.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import static dev.thural.quietspace.websocket.constant.WebSocketPaths.SYSTEM_EVENT;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SystemWebSocketController {

    @MessageMapping(SYSTEM_EVENT)
    @SendTo(SYSTEM_EVENT)
    SystemEvent handleSystemEvent(SystemEvent event) {
        log.info("system event: {} [{}] - {}", event.getType(), event.getSeverity(), event.getMessage());
        return event;
    }

}
