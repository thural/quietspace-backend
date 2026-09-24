package dev.thural.quietspace.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatService;
import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "user", roles = "USER", authorities = "USER, ADMIN")
class ChatControllerTest {

    MockMvc mockMvc;

    @Spy
    ObjectMapper objectMapper;

    @Mock
    ChatService chatService;

    @InjectMocks
    ChatController chatController;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    ArgumentCaptor<CreateChatRequest> chatRequestArgumentCaptor;

    private User user1;
    private User user2;
    private UserResponse userResponse1;
    private UserResponse userResponse2;
    private Chat chat;
    private CreateChatRequest chatRequest;
    private ChatResponse chatResponse;
    private MessageRequest messageRequest;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();

        this.user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@test.com")
                .password("password")
                .role(Role.USER)
                .build();
        this.user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@test.com")
                .password("password")
                .role(Role.USER)
                .build();

        this.userResponse1 = UserResponse.builder()
                .id(user1.getId())
                .username(user1.getUsername())
                .email(user1.getEmail())
                .role(user1.getRole().name())
                .build();
        this.userResponse2 = UserResponse.builder()
                .id(user2.getId())
                .username(user2.getUsername())
                .email(user2.getEmail())
                .role(user2.getRole().name())
                .build();

        this.chat = Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of(user1, user2))
                .messages(List.of())
                .build();

        this.chatRequest = CreateChatRequest.builder()
                .userIds(List.of(user1.getId(), user2.getId()))
                .build();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .sender(user1)
                .chat(chat)
                .text("sample text")
                .build();

        this.messageResponse = MessageResponse.builder()
                .id(message.getId())
                .chatId(chat.getId())
                .text(message.getText())
                .build();

        this.chatResponse = ChatResponse.builder()
                .id(chat.getId())
                .members(List.of(userResponse1, userResponse2))
                .recentMessage(messageResponse)
                .build();
    }

    @Test
    void getSingleChatById() throws Exception {
        when(chatService.getChatById(any())).thenReturn(chatResponse);

        mockMvc.perform(get(ChatController.CHAT_PATH + "/" + chat.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(chat.getId().toString())))
                .andExpect(jsonPath("$.recentMessage.text", is(messageResponse.getText())))
                .andExpect(jsonPath("$.members.size()", is(chatResponse.getMembers().size())))
                .andExpect(jsonPath("$.members[0].id", is(chatResponse.getMembers().get(0).getId().toString())))
                .andExpect(jsonPath("$.members[0].username", is(chatResponse.getMembers().get(0).getUsername())))
                .andExpect(status().isOk());

        verify(chatService, times(1)).getChatById(uuidArgumentCaptor.capture());
        assertThat(chatResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());

    }

    @Test
    void getChatsByMemberId() throws Exception {
        when(chatService.getChatsByUserId(any())).thenReturn(List.of(chatResponse));

        mockMvc.perform(get(ChatController.CHAT_PATH + "/members/" + chat.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id", is(chat.getId().toString())))
                .andExpect(jsonPath("$[0].recentMessage.text", is(messageResponse.getText())))
                .andExpect(jsonPath("$[0].members.size()", is(chatResponse.getMembers().size())))
                .andExpect(jsonPath("$[0].members[0].id", is(chatResponse.getMembers().get(0).getId().toString())))
                .andExpect(jsonPath("$[0].members[0].username", is(chatResponse.getMembers().get(0).getUsername())))
                .andExpect(status().isOk());

        verify(chatService, times(1)).getChatsByUserId(uuidArgumentCaptor.capture());
        assertThat(chatResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void createChat() throws Exception {
        when(chatService.createChat(any())).thenReturn(chatResponse);
        mockMvc.perform(post(ChatController.CHAT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(jsonPath("$.id", is(chat.getId().toString())))
                .andExpect(jsonPath("$.recentMessage.text", is(messageResponse.getText())))
                .andExpect(jsonPath("$.members.size()", is(chatResponse.getMembers().size())))
                .andExpect(jsonPath("$.members[0].id", is(chatResponse.getMembers().get(0).getId().toString())))
                .andExpect(jsonPath("$.members[0].username", is(chatResponse.getMembers().get(0).getUsername())))
                .andExpect(status().isOk());

        verify(chatService).createChat(chatRequestArgumentCaptor.capture());
        assertThat(chatRequest.getUserIds().get(0)).isEqualTo(chatRequestArgumentCaptor.getValue().getUserIds().get(0));
    }

    @Test
    void addMemberWithId() throws Exception {
        when(chatService.addMemberWithId(any(UUID.class), any(UUID.class))).thenReturn(userResponse1);
        mockMvc.perform(patch(ChatController.CHAT_PATH + "/" + chat.getId() + "/members/add/" + user1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userResponse1.getId().toString())))
                .andExpect(jsonPath("$.role", is(userResponse1.getRole())))
                .andExpect(jsonPath("$.username", is(userResponse1.getUsername())))
                .andExpect(jsonPath("$.email", is(userResponse1.getEmail())))
                .andExpect(status().isOk());

        verify(chatService).addMemberWithId(user1.getId(), chat.getId());
    }

    @Test
    void removeMemberWithId() throws Exception {
        mockMvc.perform(patch(ChatController.CHAT_PATH + "/" + chat.getId() + "/members/remove/" + user1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(chatService).removeMemberWithId(user1.getId(), chat.getId());
    }

    @Test
    void deleteChatWithId() throws Exception {
        mockMvc.perform(delete(ChatController.CHAT_PATH + "/" + chat.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(chatService).deleteChatById(chat.getId());
    }
}
