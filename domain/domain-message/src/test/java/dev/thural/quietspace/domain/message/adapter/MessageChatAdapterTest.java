package dev.thural.quietspace.domain.message.adapter;

import dev.thural.quietspace.domain.chat.Chat;
import dev.thural.quietspace.domain.message.Message;
import dev.thural.quietspace.domain.message.MessageRepository;
import dev.thural.quietspace.domain.message.MessageService;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import dev.thural.quietspace.domain.message.dto.MessageResponse;
import dev.thural.quietspace.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageChatAdapterTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageChatAdapter adapter;

    private final UUID chatId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Message message() {
        return Message.builder()
                .id(UUID.randomUUID())
                .chat(Chat.builder().id(chatId).build())
                .sender(User.builder().id(userId).username("u").build())
                .recipient(User.builder().id(UUID.randomUUID()).username("r").build())
                .text("hello")
                .isSeen(false)
                .build();
    }

    @Test
    void findLastMessage_givenMessages_shouldReturnView() {
        Message message = message();
        when(messageRepository.findAllByChatId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(message)));

        var result = adapter.findLastMessage(chatId);

        assertThat(result).isPresent();
        assertThat(result.get().getText()).isEqualTo("hello");
        assertThat(result.get().getChatId()).isEqualTo(chatId);
    }

    @Test
    void findLastMessage_givenNoMessages_shouldBeEmpty() {
        when(messageRepository.findAllByChatId(any(), any())).thenReturn(Page.empty());

        assertThat(adapter.findLastMessage(chatId)).isEmpty();
    }

    @Test
    void postFirstMessage_shouldPostAndReturnView() {
        Message message = message();
        when(messageService.addMessage(any(MessageRequest.class)))
                .thenReturn(MessageResponse.builder().id(message.getId()).build());
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        var view = adapter.postFirstMessage(chatId, userId, UUID.randomUUID(), "hello");

        assertThat(view.getId()).isEqualTo(message.getId());
        assertThat(view.getText()).isEqualTo("hello");
    }
}
