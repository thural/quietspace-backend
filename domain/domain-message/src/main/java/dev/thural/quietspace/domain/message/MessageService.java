package dev.thural.quietspace.domain.message;

import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import dev.thural.quietspace.domain.message.dto.MessageResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface MessageService {

    MessageResponse addMessage(MessageRequest messageRequest);

    Optional<MessageResponse> deleteMessage(UUID id);

    Page<MessageResponse> getMessagesByChatId(Integer pageNumber, Integer pageSize, UUID chatId);

    Optional<MessageResponse> getLastMessageByChat(Chat chat);

    Optional<MessageResponse> setMessageSeen(UUID messageId);

    MessageResponse getMessageById(UUID messageId);

    MessageResponse getMessageById(UUID messageId, UUID chatId);

    long getUnreadCount();
}
