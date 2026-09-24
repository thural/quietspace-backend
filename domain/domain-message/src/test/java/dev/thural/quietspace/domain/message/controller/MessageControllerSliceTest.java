package dev.thural.quietspace.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.MessageService;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.post.PostService;
import dev.thural.quietspace.reaction.ReactionService;
import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.security.TokenRepository;
import dev.thural.quietspace.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = MessageController.class)
class MessageControllerSliceTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;
    @MockitoBean
    private ReactionService reactionService;
    @MockitoBean
    PostService postService;
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    TokenRepository tokenRepository;
    @MockitoBean
    UserDetailsService userDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<MessageRequest> messageRequestArgumentCaptor = ArgumentCaptor.forClass(MessageRequest.class);

    private MessageRequest messageRequest;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@email.com")
                .password("pAsSwoRd")
                .build();

        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .build();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .text("sample text")
                .chat(chat)
                .sender(user)
                .build();
        this.messageResponse = MessageResponse.builder()
                .id(UUID.randomUUID())
                .text("sample text")
                .chatId(chat.getId())
                .senderId(user.getId())
                .senderName(user.getUsername())
                .build();

        this.messageRequest = MessageRequest.builder()
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .recipientId(user2.getId())
                .text(message.getText())
                .build();
    }

    @Test
    void createMessage() throws Exception {
        when(messageService.addMessage(any())).thenReturn(messageResponse);

        mockMvc.perform(multipart(MessageController.MESSAGE_PATH)
                        .file(new MockMultipartFile("messageRequest", "", "application/json", objectMapper.writeValueAsString(messageRequest).getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("photoData", "photo.jpg", "image/jpeg", "photo-content".getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(jsonPath("$.senderName", is(messageResponse.getSenderName())))
                .andExpect(jsonPath("$.text", is(messageResponse.getText())))
                .andExpect(jsonPath("$.id", is(messageResponse.getId().toString())))
                .andExpect(jsonPath("$.senderId", is(messageResponse.getSenderId().toString())))
                .andExpect(jsonPath("$.chatId", is(messageResponse.getChatId().toString())))
                .andExpect(status().isOk());

        verify(messageService, times(1)).addMessage(any());
    }

    @Test
    void createMessageWithoutPhoto() throws Exception {
        when(messageService.addMessage(any())).thenReturn(messageResponse);

        mockMvc.perform(multipart(MessageController.MESSAGE_PATH)
                        .file(new MockMultipartFile("messageRequest", "", "application/json", objectMapper.writeValueAsString(messageRequest).getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(jsonPath("$.senderName", is(messageResponse.getSenderName())))
                .andExpect(jsonPath("$.text", is(messageResponse.getText())))
                .andExpect(jsonPath("$.id", is(messageResponse.getId().toString())))
                .andExpect(jsonPath("$.senderId", is(messageResponse.getSenderId().toString())))
                .andExpect(jsonPath("$.chatId", is(messageResponse.getChatId().toString())))
                .andExpect(status().isOk());

        verify(messageService, times(1)).addMessage(any());
    }

    @Test
    void createMessageInvalidPayload() throws Exception {
        MessageRequest invalidRequest = MessageRequest.builder()
                .chatId(null)
                .senderId(null)
                .recipientId(null)
                .text("")
                .build();

        mockMvc.perform(multipart(MessageController.MESSAGE_PATH)
                        .file(new MockMultipartFile("messageRequest", "", "application/json", objectMapper.writeValueAsString(invalidRequest).getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMessage() throws Exception {
        mockMvc.perform(delete(MessageController.MESSAGE_PATH + "/" + messageResponse.getId()))
                .andExpect(status().isNoContent());

        verify(messageService).deleteMessage(uuidArgumentCaptor.capture());
        assertThat(messageResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void getUnreadCount() throws Exception {
        when(messageService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get(MessageController.MESSAGE_PATH + "/unread")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(3)))
                .andExpect(status().isOk());

        verify(messageService).getUnreadCount();
    }

    @Test
    void markAsRead() throws Exception {
        when(messageService.setMessageSeen(any())).thenReturn(Optional.of(messageResponse));

        mockMvc.perform(put(MessageController.MESSAGE_PATH + "/" + messageResponse.getId() + "/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(messageResponse.getId().toString())))
                .andExpect(status().isOk());

        verify(messageService).setMessageSeen(messageResponse.getId());
    }

    @Test
    void markAsRead_givenNonExistent_shouldReturn404() throws Exception {
        when(messageService.setMessageSeen(any())).thenReturn(Optional.empty());

        mockMvc.perform(put(MessageController.MESSAGE_PATH + "/" + UUID.randomUUID() + "/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessagesByChatId() throws Exception {
        when(messageService.getMessagesByChatId(any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get(MessageController.MESSAGE_PATH + "/chat/" + messageResponse.getId())
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(messageService).getMessagesByChatId(1, 10, messageResponse.getId());
    }

}
