package dev.thural.quietspace.chat;

import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.chat.dto.UpdateChatRequest;
import dev.thural.quietspace.user.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    List<ChatResponse> getChatsByUserId(UUID userId);

    void deleteChatById(UUID chatId);

    UserResponse addMemberWithId(UUID memberId, UUID chatId);

    List<UserResponse> removeMemberWithId(UUID memberId, UUID chatId);

    ChatResponse createChat(CreateChatRequest chatRequest);

    ChatResponse getChatById(UUID chatId);

    ChatResponse updateChat(UUID chatId, UpdateChatRequest request);

}
