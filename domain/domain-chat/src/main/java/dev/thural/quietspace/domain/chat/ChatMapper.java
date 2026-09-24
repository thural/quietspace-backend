package dev.thural.quietspace.domain.chat;

import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.chat.dto.ChatMessageView;
import dev.thural.quietspace.domain.chat.dto.ChatResponse;
import dev.thural.quietspace.domain.chat.dto.CreateChatRequest;
import dev.thural.quietspace.domain.chat.port.ChatMessagePort;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserMapper;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMapper {
    private final UserMapper userMapper;
    private final UserService userService;
    private final ChatMessagePort messagePort;

    public Chat chatRequestToEntity(CreateChatRequest chatRequest) {
        return Chat.builder()
                .users(getUserListFromRequest(chatRequest))
                .build();
    }

    public ChatResponse chatEntityToResponse(Chat chat) {
        return ChatResponse.builder()
                .id(chat.getId())
                .userIds(getUserIdsFromChat(chat))
                .members(getChatMembers(chat))
                .recentMessage(getLastMessage(chat))
                .createDate(chat.getCreateDate())
                .updateDate(chat.getUpdateDate())
                .build();
    }

    private ChatMessageView getLastMessage(Chat chat) {
        return messagePort.findLastMessage(chat.getId()).orElse(null);
    }

    private List<UUID> getUserIdsFromChat(Chat chat) {
        return chat.getUsers().stream().map(User::getId).toList();
    }

    private List<User> getUserListFromRequest(CreateChatRequest chatRequest) {
        return userService.getUsersFromIdList(chatRequest.getUserIds());
    }

    private List<UserResponse> getChatMembers(Chat chat) {
        User loggedUser = userService.getSignedUser();
        return chat.getUsers().stream()
                .filter(user -> !user.equals(loggedUser))
                .map(userMapper::toResponse).toList();
    }
}
