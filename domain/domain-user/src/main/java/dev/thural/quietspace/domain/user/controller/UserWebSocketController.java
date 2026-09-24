package dev.thural.quietspace.domain.controller;

import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.dto.UserResponse;
import dev.thural.quietspace.core.messaging.model.UserRepresentation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

import static dev.thural.quietspace.core.shared.enums.StatusType.OFFLINE;
import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserWebSocketController {

    private final UserService userService;
    private final SimpMessagingTemplate template;

    @MessageMapping(SET_ONLINE_STATUS)
    @SendTo(USER_PUBLIC)
    public UserRepresentation goOffline(@Payload @Valid UserRepresentation user) {
        userService.setOnlineStatus(user.getEmail(), OFFLINE);
        return user;
    }

    @MessageMapping(ONLINE_USERS)
    public void getOnlineUsers() {
        User signedUser = userService.getSignedUser();
        List<UserResponse> onlineUsers = userService.findConnectedFollowings();
        template.convertAndSendToUser(signedUser.getId().toString(), ONLINE_USERS, onlineUsers);
    }

}
