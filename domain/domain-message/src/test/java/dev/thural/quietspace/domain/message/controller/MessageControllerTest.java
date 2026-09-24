package dev.thural.quietspace.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.MessageService;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "user", roles = "USER", authorities = "USER, ADMIN")
class MessageControllerTest {

    MockMvc mockMvc;
    @Spy
    ObjectMapper objectMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    MessageController messageController;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    ArgumentCaptor<MessageRequest> messageRequestArgumentCaptor;

    private MessageRequest messageRequest;
    private MessageResponse messageResponse;
    private UUID chatId;

    @BeforeEach
    void setUp() {

        this.mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .role(Role.ADMIN)
                .password("pAsSword")
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user2")
                .email("user2@email.com")
                .role(Role.ADMIN)
                .password("pAsSWord")
                .build();

        Chat chat = Chat.builder()
                .id(UUID.randomUUID())
                .build();

        this.chatId = chat.getId();

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

        verify(messageService).addMessage(messageRequestArgumentCaptor.capture());
        assertThat(messageResponse.getText()).isEqualTo(messageRequestArgumentCaptor.getValue().getText());
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

        verify(messageService).addMessage(messageRequestArgumentCaptor.capture());
        assertThat(messageRequestArgumentCaptor.getValue().getPhotoData()).isNull();
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
    void getMessagesByChatId() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(messageService.getMessagesByChatId(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get(MessageController.MESSAGE_PATH + "/chat/" + chatId)
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(messageService).getMessagesByChatId(1, 10, chatId);
    }

}
