package dev.thural.quietspace.chat;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatMapper;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.chat.ChatServiceImpl;
import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.chat.dto.UpdateChatRequest;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.MessageRepository;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserMapper;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private UUID userId;
    private UUID memberId;
    private User user1;
    private UserResponse userResponse;
    private List<User> userList;
    private User user2;
    private Chat chat;
    private ChatResponse chatResponse;
    private CreateChatRequest chatRequest;

    @BeforeEach
    void initMockData() {
        this.userId = UUID.randomUUID();
        this.memberId = UUID.randomUUID();

        this.user1 = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .build();

        this.userResponse = UserResponse.builder()
                .id(user1.getId())
                .build();

        this.user2 = User.builder()
                .id(userId)
                .username("member")
                .email("member@email.com")
                .password("pAsSWord")
                .build();

        this.userList = new ArrayList<>();
        userList.add(user1);

        this.chat = Chat.builder()
                .id(UUID.randomUUID())
                .users(userList)
                .messages(List.of())
                .build();

        this.chatRequest = CreateChatRequest.builder()
                .userIds(List.of(userId, memberId))
                .recipientId(userId)
                .text("hello")
                .build();

        this.chatResponse = new ChatResponse();
    }

    @Test
    void findChatById_shouldReturnChat() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        Chat foundChat = chatService.findChatEntityById(chat.getId());

        assertThat(foundChat).isEqualTo(chat);
        verify(userService, times(1)).getSignedUser();
        verify(chatRepository, times(1)).findById(chat.getId());
    }

    @Test
    void getChatsByUserId_shouldReturnChats() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findAllByUsersId(userId)).thenReturn(List.of(chat));
        when(chatMapper.chatEntityToResponse(any(Chat.class))).thenReturn(chatResponse);

        List<ChatResponse> chats = chatService.getChatsByUserId(userId);

        assertThat(chats).isEqualTo(List.of(chatResponse));
        verify(userService, times(1)).getSignedUser();
        verify(chatMapper, times(1)).chatEntityToResponse(any(Chat.class));
        verify(chatRepository, times(1)).findAllByUsersId(userId);
    }

    @Test
    void deleteChatById_shouldSucceed() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));

        chatService.deleteChatById(chat.getId());

        verify(userService, times(1)).getSignedUser();
        verify(chatRepository, times(1)).deleteById(chat.getId());
    }

    @Test
    void addMember_shouldReturnUpdatedChat() {
        UserResponse memberResponse = new UserResponse();
        BeanUtils.copyProperties(user2, memberResponse);

        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(userService.getUserById(memberId)).thenReturn(Optional.of(user2));
        when(userMapper.toResponse(user2)).thenReturn(memberResponse);

        UserResponse addedUser = chatService.addMemberWithId(memberId, chat.getId());

        assertThat(addedUser).isEqualTo(memberResponse);
        verify(userMapper, times(1)).toResponse(user2);
    }

    @Test
    void removeMember_shouldReturnUpdatedChat() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(userService.getUserById(userId)).thenReturn(Optional.of(user1));
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        chatService.removeMemberWithId(userId, chat.getId());

        verify(userService, times(1)).getSignedUser();
        verify(chatRepository, times(1)).save(chat);
    }

    @Test
    void createChat_shouldReturnChat() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(userService.getUsersFromIdList(anyList())).thenReturn(userList);
        when(userService.getUserById(chatRequest.getRecipientId())).thenReturn(Optional.of(user1));
        when(chatRepository.findAllByUsersIn(userList)).thenReturn(List.of());
        when(chatMapper.chatEntityToResponse(chat)).thenReturn(chatResponse);
        when(chatMapper.chatRequestToEntity(chatRequest)).thenReturn(chat);
        when(chatRepository.save(chat)).thenReturn(chat);
        when(messageRepository.save(any(Message.class))).thenReturn(null);

        ChatResponse createdChat = chatService.createChat(chatRequest);

        assertThat(createdChat).isEqualTo(chatResponse);
        verify(chatRepository, times(1)).findAllByUsersIn(userList);
        verify(chatRepository, times(1)).save(chat);
    }

    @Test
    void getChatById_givenValidId_shouldReturnResponse() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(chatMapper.chatEntityToResponse(chat)).thenReturn(chatResponse);

        ChatResponse result = chatService.getChatById(chat.getId());

        assertThat(result).isEqualTo(chatResponse);
    }

    @Test
    void updateChat_shouldUpdateName() {
        UpdateChatRequest request = UpdateChatRequest.builder().name("new name").build();
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(chatRepository.save(chat)).thenReturn(chat);
        when(chatMapper.chatEntityToResponse(chat)).thenReturn(chatResponse);

        ChatResponse result = chatService.updateChat(chat.getId(), request);

        assertThat(chat.getName()).isEqualTo("new name");
        assertThat(result).isEqualTo(chatResponse);
        verify(chatRepository).save(chat);
    }

    @Test
    void updateChat_givenNullName_shouldNotChange() {
        UpdateChatRequest request = UpdateChatRequest.builder().name(null).build();
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(chat.getId())).thenReturn(Optional.of(chat));
        when(chatRepository.save(chat)).thenReturn(chat);
        when(chatMapper.chatEntityToResponse(chat)).thenReturn(chatResponse);

        ChatResponse result = chatService.updateChat(chat.getId(), request);

        assertThat(chat.getName()).isNull();
        assertThat(result).isEqualTo(chatResponse);
        verify(chatRepository).save(chat);
    }

    @Test
    void getChatById_givenNonExistentChat_shouldThrow() {
        when(userService.getSignedUser()).thenReturn(user1);
        when(chatRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

}