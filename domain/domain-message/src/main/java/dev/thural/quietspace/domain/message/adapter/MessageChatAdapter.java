package dev.thural.quietspace.domain.message.adapter;

import dev.thural.quietspace.domain.chat.dto.ChatMessageView;
import dev.thural.quietspace.domain.chat.port.ChatMessagePort;
import dev.thural.quietspace.domain.message.Message;
import dev.thural.quietspace.domain.message.MessageRepository;
import dev.thural.quietspace.domain.message.MessageService;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Message-domain implementation of the chat-owned {@link ChatMessagePort}.
 */
@Component
@RequiredArgsConstructor
public class MessageChatAdapter implements ChatMessagePort {

    private final MessageRepository messageRepository;
    private final MessageService messageService;

    @Override
    public Optional<ChatMessageView> findLastMessage(UUID chatId) {
        return messageRepository.findAllByChatId(chatId,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createDate")))
                .stream().findFirst().map(this::toView);
    }

    @Override
    public ChatMessageView postFirstMessage(UUID chatId, UUID senderId, UUID recipientId, String text) {
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setSenderId(senderId);
        request.setRecipientId(recipientId);
        request.setText(text);
        UUID messageId = messageService.addMessage(request).getId();
        Message saved = messageRepository.findById(messageId).orElseThrow();
        return toView(saved);
    }

    private ChatMessageView toView(Message message) {
        return ChatMessageView.builder()
                .id(message.getId())
                .chatId(message.getChat() != null ? message.getChat().getId() : null)
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .text(message.getText())
                .isSeen(message.getIsSeen())
                .photoId(message.getPhotoId())
                .build();
    }
}
