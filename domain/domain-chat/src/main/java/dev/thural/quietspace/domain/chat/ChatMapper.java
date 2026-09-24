package dev.thural.quietspace.chat;

import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.message.MessageService;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserMapper;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMapper {
    private final UserMapper userMapper;
    private final UserService userService;
    private final MessageService messageService;

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

    private MessageResponse getLastMessage(Chat chat) {
        return messageService.getLastMessageByChat(chat).orElse(null);
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
