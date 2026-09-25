package dev.thural.quietspace.domain.message.controller;

import dev.thural.quietspace.core.messaging.event.EventType;
import dev.thural.quietspace.core.messaging.event.message.ChatEvent;
import dev.thural.quietspace.domain.chat.ChatService;
import dev.thural.quietspace.domain.chat.dto.TypingStatus;
import dev.thural.quietspace.domain.message.MessageService;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import dev.thural.quietspace.domain.message.dto.MessageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private ChatWebSocketController controller;

    private final UUID chatId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    private MessageResponse messageResponse() {
        return MessageResponse.builder()
                .id(messageId)
                .chatId(chatId)
                .senderId(userId)
                .senderName("user")
                .text("hello")
                .build();
    }

    @Test
    void sendMessageToAll_shouldEcho() {
        MessageRequest request = MessageRequest.builder().text("hi").build();

        assertThat(controller.sendMessageToAll(request)).isSameAs(request);
    }

    @Test
    void sendMessageToUser_shouldDelegate() {
        MessageRequest request = MessageRequest.builder().text("hi").build();
        when(messageService.addMessage(request)).thenReturn(messageResponse());

        assertThat(controller.sendMessageToUser(request).getId()).isEqualTo(messageId);
    }

    @Test
    void deleteMessageById_shouldReturnEvent() {
        when(messageService.getMessageById(messageId)).thenReturn(messageResponse());
        when(messageService.deleteMessage(messageId)).thenReturn(Optional.of(messageResponse()));

        ChatEvent event = controller.deleteMessageById(messageId);

        assertThat(event.getType()).isEqualTo(EventType.DELETE_MESSAGE);
        assertThat(event.getChatId()).isEqualTo(chatId);
    }

    @Test
    void deleteMessageById_givenDeleteFails_shouldReturnExceptionEvent() {
        when(messageService.getMessageById(messageId)).thenReturn(messageResponse());
        when(messageService.deleteMessage(messageId)).thenReturn(Optional.empty());

        ChatEvent event = controller.deleteMessageById(messageId);

        assertThat(event.getType()).isEqualTo(EventType.EXCEPTION);
    }

    @Test
    void markMessageSeen_shouldReturnEvent() {
        when(messageService.setMessageSeen(messageId)).thenReturn(Optional.of(messageResponse()));

        ChatEvent event = controller.markMessageSeen(messageId);

        assertThat(event.getType()).isEqualTo(EventType.SEEN_MESSAGE);
        assertThat(event.getMessageId()).isEqualTo(messageId);
    }

    @Test
    void markMessageSeen_givenMissing_shouldThrow() {
        when(messageService.setMessageSeen(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.markMessageSeen(messageId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void processLeftChat_shouldRemoveMember() {
        ChatEvent event = ChatEvent.builder().chatId(chatId).actorId(userId).build();

        ChatEvent result = controller.processLeftChat(event);

        assertThat(result.getType()).isEqualTo(EventType.LEFT_CHAT);
    }

    @Test
    void processLeftChat_givenFailure_shouldReturnExceptionEvent() {
        doThrow(new RuntimeException("gone")).when(chatService).removeMemberWithId(any(), any());
        ChatEvent event = ChatEvent.builder().chatId(chatId).actorId(userId).build();

        ChatEvent result = controller.processLeftChat(event);

        assertThat(result.getType()).isEqualTo(EventType.EXCEPTION);
    }

    @Test
    void handleTypingStatus_shouldEcho() {
        TypingStatus status = TypingStatus.builder().userId(userId).chatId(chatId).isTyping(true).build();

        assertThat(controller.handleTypingStatus(status)).isSameAs(status);
    }

    @Test
    void processJoinChat_shouldAddMember() {
        ChatEvent event = ChatEvent.builder().chatId(chatId).actorId(userId).recipientId(userId).build();

        ChatEvent result = controller.processJoinChat(event);

        assertThat(result.getType()).isEqualTo(EventType.JOINED_CHAT);
    }

    @Test
    void processJoinChat_givenFailure_shouldReturnExceptionEvent() {
        doThrow(new RuntimeException("gone")).when(chatService).addMemberWithId(any(), any());
        ChatEvent event = ChatEvent.builder().chatId(chatId).actorId(userId).recipientId(userId).build();

        ChatEvent result = controller.processJoinChat(event);

        assertThat(result.getType()).isEqualTo(EventType.EXCEPTION);
    }
}
