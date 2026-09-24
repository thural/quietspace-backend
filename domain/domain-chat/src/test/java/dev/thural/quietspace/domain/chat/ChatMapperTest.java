package dev.thural.quietspace.chat;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatMapper;
import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.message.MessageService;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserMapper;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMapperTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ChatMapper chatMapper;

    private CreateChatRequest createChatRequest;
    private Chat chat;
    private User user1;
    private User user2;
    private User loggedUser;
    private MessageResponse recentMessage;
    private UserResponse userResponse1;
    private UserResponse userResponse2;

    @BeforeEach
    void setUp() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID loggedUserId = UUID.randomUUID();

        user1 = User.builder()
                .id(userId1)
                .username("user1")
                .email("user1@test.com")
                .build();

        user2 = User.builder()
                .id(userId2)
                .username("user2")
                .email("user2@test.com")
                .build();

        loggedUser = User.builder()
                .id(loggedUserId)
                .username("loggedUser")
                .email("logged@test.com")
                .build();

        createChatRequest = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(userId2)
                .text("Hello")
                .userIds(List.of(userId1, userId2))
                .build();

        chat = Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of(user1, user2, loggedUser))
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        recentMessage = MessageResponse.builder()
                .id(UUID.randomUUID())
                .text("Recent message")
                .senderId(userId1)
                .build();

        userResponse1 = UserResponse.builder()
                .id(userId1)
                .username("user1")
                .build();

        userResponse2 = UserResponse.builder()
                .id(userId2)
                .username("user2")
                .build();
    }

    @Test
    void chatRequestToEntity_shouldConvertRequestToEntity() {
        // Given
        List<User> expectedUsers = List.of(user1, user2);
        when(userService.getUsersFromIdList(createChatRequest.getUserIds())).thenReturn(expectedUsers);

        // When
        Chat result = chatMapper.chatRequestToEntity(createChatRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsers()).isEqualTo(expectedUsers);
        verify(userService).getUsersFromIdList(createChatRequest.getUserIds());
    }

    @Test
    void chatRequestToEntity_shouldHandleNullUserIds() {
        // Given
        CreateChatRequest requestWithNullIds = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(UUID.randomUUID())
                .text("Hello")
                .userIds(null)
                .build();
        when(userService.getUsersFromIdList(null)).thenReturn(List.of());

        // When
        Chat result = chatMapper.chatRequestToEntity(requestWithNullIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsers()).isEmpty();
        verify(userService).getUsersFromIdList(null);
    }

    @Test
    void chatEntityToResponse_shouldConvertEntityToResponse() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(userMapper.toResponse(user1)).thenReturn(userResponse1);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);
        when(messageService.getLastMessageByChat(chat)).thenReturn(Optional.of(recentMessage));

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(chat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(chat.getId());
        assertThat(result.getCreateDate()).isEqualTo(chat.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(chat.getUpdateDate());
        assertThat(result.getUserIds()).containsExactly(user1.getId(), user2.getId(), loggedUser.getId());
        assertThat(result.getMembers()).hasSize(2); // Excludes logged user
        assertThat(result.getRecentMessage()).isEqualTo(recentMessage);
        
        verify(userService).getSignedUser();
        verify(userMapper).toResponse(user1);
        verify(userMapper).toResponse(user2);
        verify(messageService).getLastMessageByChat(chat);
    }

    @Test
    void chatEntityToResponse_shouldHandleNullRecentMessage() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(userMapper.toResponse(user1)).thenReturn(userResponse1);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);
        when(messageService.getLastMessageByChat(chat)).thenReturn(Optional.empty());

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(chat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecentMessage()).isNull();
        verify(messageService).getLastMessageByChat(chat);
    }

    @Test
    void chatEntityToResponse_shouldFilterOutLoggedUserFromMembers() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(userMapper.toResponse(user1)).thenReturn(userResponse1);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);
        when(messageService.getLastMessageByChat(chat)).thenReturn(Optional.empty());

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(chat);

        // Then
        assertThat(result.getMembers()).hasSize(2);
        assertThat(result.getMembers()).containsExactly(userResponse1, userResponse2);
        assertThat(result.getMembers()).doesNotContainAnyElementsOf(List.of(
                UserResponse.builder().id(loggedUser.getId()).build()));
    }

    @Test
    void chatEntityToResponse_shouldHandleEmptyUsersList() {
        // Given
        Chat emptyChat = Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(messageService.getLastMessageByChat(emptyChat)).thenReturn(Optional.empty());

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(emptyChat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserIds()).isEmpty();
        assertThat(result.getMembers()).isEmpty();
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void chatEntityToResponse_shouldHandleSingleUserChat() {
        // Given
        Chat singleUserChat = Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of(loggedUser))
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(messageService.getLastMessageByChat(singleUserChat)).thenReturn(Optional.empty());

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(singleUserChat);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserIds()).containsExactly(loggedUser.getId());
        assertThat(result.getMembers()).isEmpty(); // Only logged user, so members list is empty
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getLastMessage_shouldReturnNullWhenNoMessage() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);
        when(userMapper.toResponse(user1)).thenReturn(userResponse1);
        when(userMapper.toResponse(user2)).thenReturn(userResponse2);
        when(messageService.getLastMessageByChat(chat)).thenReturn(Optional.empty());

        // When
        ChatResponse result = chatMapper.chatEntityToResponse(chat);

        // Then
        assertThat(result.getRecentMessage()).isNull();
        verify(messageService).getLastMessageByChat(chat);
    }
}
