package dev.thural.quietspace.message;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.MessageMapper;
import dev.thural.quietspace.message.MessageRepository;
import dev.thural.quietspace.message.MessageServiceImpl;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.shared.util.PagingProvider.buildPageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private UserService userService;

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private PhotoService photoService;

    @Mock
    private SimpMessagingTemplate template;

    @InjectMocks
    private MessageServiceImpl messageService;


    private User user;
    private Chat chat;
    private Message message;
    private MessageRequest messageRequest;
    private MessageResponse messageResponse;


    @BeforeEach
    void setUp() {
        this.user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .build();

        this.chat = Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of())
                .messages(List.of())
                .build();

        this.message = Message.builder()
                .id(UUID.randomUUID())
                .sender(user)
                .recipient(user)
                .chat(chat)
                .text("sample text")
                .build();

        this.messageRequest = MessageRequest.builder()
                .senderId(user.getId())
                .text(message.getText())
                .chatId(chat.getId())
                .build();

        this.messageResponse = new MessageResponse();
        BeanUtils.copyProperties(message, messageResponse);
        messageResponse.setSenderId(user.getId());
        messageResponse.setChatId(chat.getId());
    }

    @Test
    void addMessage_shouldReturnMessage() {
        when(messageMapper.toEntity(messageRequest)).thenReturn(message);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);
        when(userService.getSignedUser()).thenReturn(user);
        when(chatRepository.findById(messageRequest.getChatId())).thenReturn(Optional.of(chat));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse savedMessage = messageService.addMessage(messageRequest);

        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getSenderId()).isEqualTo(user.getId());
        assertThat(savedMessage.getChatId()).isEqualTo(chat.getId());
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void deleteMessage_shouldSucceed() {
        when(userService.getSignedUser()).thenReturn(user);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        messageService.deleteMessage(message.getId());

        verify(messageRepository, times(1)).findById(message.getId());
        verify(messageRepository, times(1)).deleteById(message.getId());
    }

    @Test
    void getMessagesByChatId_shouldReturnMessages() {
        PageRequest pageRequest = buildPageRequest(1, 50, null);
        when(messageRepository.findAllByChatId(chat.getId(), pageRequest)).thenReturn(Page.empty());

        Page<MessageResponse> messagePage = messageService.getMessagesByChatId(1, 50, chat.getId());

        assertThat(messagePage.getContent()).isEmpty();
        verify(messageRepository, times(1)).findAllByChatId(chat.getId(), pageRequest);
    }

    @Test
    void getLastMessageByChat_shouldReturnMessage() {
        when(messageRepository.findFirstByChatOrderByCreateDateDesc(chat)).thenReturn(Optional.ofNullable(message));
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        Optional<MessageResponse> messageResponse = messageService.getLastMessageByChat(chat);

        assertThat(messageResponse).isPresent();
        assertThat(messageResponse.get()).isInstanceOf(MessageResponse.class);
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(messageRepository.countByRecipientIdAndIsSeen(any(), anyBoolean())).thenReturn(5L);
        when(userService.getSignedUser()).thenReturn(user);

        long count = messageService.getUnreadCount();

        assertThat(count).isEqualTo(5L);
        verify(messageRepository).countByRecipientIdAndIsSeen(user.getId(), false);
    }

    @Test
    void setMessageSeen_givenExistingMessage_shouldSaveAndReturnSeenResponse() {
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);
        when(messageRepository.countByRecipientIdAndIsSeen(any(), anyBoolean())).thenReturn(0L);

        Optional<MessageResponse> result = messageService.setMessageSeen(message.getId());

        assertThat(result).isPresent();
        assertThat(message.getIsSeen()).isTrue();
        verify(messageRepository).save(message);
    }

    @Test
    void setMessageSeen_shouldPushUnreadCount() {
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);
        when(messageRepository.countByRecipientIdAndIsSeen(any(), anyBoolean())).thenReturn(0L);

        messageService.setMessageSeen(message.getId());

        verify(template).convertAndSendToUser(
                eq(user.getId().toString()),
                eq("/unread-count"),
                eq(0L)
        );
    }

    @Test
    void setMessageSeen_givenNonExistentMessage_shouldThrow() {
        when(messageRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.setMessageSeen(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getMessageById_givenValidIds_shouldReturnResponse() {
        when(messageRepository.findByMessageIdAndChatId(message.getId(), chat.getId())).thenReturn(Optional.of(message));
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        MessageResponse result = messageService.getMessageById(message.getId(), chat.getId());

        assertThat(result).isEqualTo(messageResponse);
    }

    @Test
    void getMessageById_givenMismatchedIds_shouldThrow() {
        when(messageRepository.findByMessageIdAndChatId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getMessageById(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

}