package dev.thural.quietspace.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.domain.message.dto.MessageRequest;
import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.core.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WebSocketFlowIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoService photoService;

    private IntegrationTestHelper helper;
    private String user1Jwt;
    private String user2Jwt;
    private UUID user1Id;
    private UUID user2Id;
    private UUID chatId;

    @BeforeEach
    void setUp() throws Exception {
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        user1Jwt = helper.registerAndLogin("wsuser1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("wsuser1@test.com").orElseThrow().getId();
        user2Jwt = helper.registerAndLogin("wsuser2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("wsuser2@test.com").orElseThrow().getId();

        var createChatReq = dev.thural.quietspace.domain.chat.dto.CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Chat for WS test")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String chatResponse = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/chats")
                                .header("Authorization", "Bearer " + user1Jwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createChatReq)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        chatId = UUID.fromString(objectMapper.readTree(chatResponse).get("id").asText());
    }

    private static String decodePayload(Object payload) {
        return StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap((byte[]) payload)).toString();
    }

    private StompSession connectStomp(String jwt) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        @SuppressWarnings("removal")
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        stompClient.setMessageConverter(converter);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.set("Authorization", "Bearer " + jwt);

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        return stompClient.connectAsync("ws://localhost:" + port + "/ws/raw", handshakeHeaders, connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(30, TimeUnit.SECONDS);
    }

    private void awaitSubscription(Subscription sub) throws Exception {
        Thread.sleep(2000);
    }

    private Subscription subscribeWithReceipt(StompSession session, String destination, StompFrameHandler handler) throws Exception {
        Subscription sub = session.subscribe(destination, handler);
        awaitSubscription(sub);
        return sub;
    }

    @Test
    void connectAndReceivePublicMessage_shouldWork() throws Exception {
        StompSession session = connectStomp(user1Jwt);

        CompletableFuture<String> receivedMessage = new CompletableFuture<>();

        session.subscribe("/public/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String json = decodePayload(payload);
                receivedMessage.complete(json);
            }
        });

        session.send("/app/public/chat", MessageRequest.builder()
                .chatId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .text("Hello from WebSocket test!")
                .build());

        String result = receivedMessage.get(30, TimeUnit.SECONDS);
        assertThat(result).contains("Hello from WebSocket test!");
    }

    @Test
    void sendPrivateMessage_shouldDeliver() throws Exception {
        StompSession user1Session = connectStomp(user1Jwt);
        StompSession user2Session = connectStomp(user2Jwt);

        CompletableFuture<String> user2Received = new CompletableFuture<>();

        subscribeWithReceipt(user2Session, "/public/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                user2Received.complete(decodePayload(payload));
            }
        });

        user1Session.send("/app/private/chat", MessageRequest.builder()
                .chatId(chatId)
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Private message test")
                .build());

        String result = user2Received.get(30, TimeUnit.SECONDS);
        assertThat(result).contains("Private message test");
    }

    @Test
    void deleteMessage_shouldNotifyParticipants() throws Exception {
        StompSession user1Session = connectStomp(user1Jwt);
        StompSession user2Session = connectStomp(user2Jwt);

        CompletableFuture<String> eventReceived = new CompletableFuture<>();

        subscribeWithReceipt(user2Session, "/private/chat/event", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eventReceived.complete(decodePayload(payload));
            }
        });

        CompletableFuture<String> messageSent = new CompletableFuture<>();

        subscribeWithReceipt(user1Session, "/public/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageSent.complete(decodePayload(payload));
            }
        });

        user1Session.send("/app/private/chat", MessageRequest.builder()
                .chatId(chatId)
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Message to delete")
                .build());

        String sentJson = messageSent.get(30, TimeUnit.SECONDS);
        String messageId = objectMapper.readTree(sentJson).get("id").asText();

        user1Session.send("/app/private/chat/delete/" + messageId, null);

        String event = eventReceived.get(30, TimeUnit.SECONDS);
        assertThat(event).contains("DELETE_MESSAGE");
    }

    @Test
    void markMessageAsSeen_shouldNotifyParticipants() throws Exception {
        StompSession user1Session = connectStomp(user1Jwt);
        StompSession user2Session = connectStomp(user2Jwt);

        CompletableFuture<String> eventReceived = new CompletableFuture<>();

        subscribeWithReceipt(user1Session, "/private/chat/event", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                eventReceived.complete(decodePayload(payload));
            }
        });

        CompletableFuture<String> messageSent = new CompletableFuture<>();

        subscribeWithReceipt(user1Session, "/public/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageSent.complete(decodePayload(payload));
            }
        });

        user1Session.send("/app/private/chat", MessageRequest.builder()
                .chatId(chatId)
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Message to mark seen")
                .build());

        String sentJson = messageSent.get(30, TimeUnit.SECONDS);
        String messageId = objectMapper.readTree(sentJson).get("id").asText();

        user2Session.send("/app/private/chat/seen/" + messageId, null);

        String event = eventReceived.get(30, TimeUnit.SECONDS);
        assertThat(event).contains("SEEN_MESSAGE");
    }

    @Test
    void leaveChat_shouldNotifyParticipants() throws Exception {
        StompSession user1Session = connectStomp(user1Jwt);
        StompSession user2Session = connectStomp(user2Jwt);

        CompletableFuture<String> notification = new CompletableFuture<>();

        Predicate<String> leavesMatcher = json -> json.contains("LEFT_CHAT") || json.contains("left the chat");

        StompFrameHandler handler = new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String json = decodePayload(payload);
                if (leavesMatcher.test(json)) {
                    notification.complete(json);
                }
            }
        };

        subscribeWithReceipt(user2Session, "/public/chat", handler);
        subscribeWithReceipt(user1Session, "/public/chat", handler);

        var chatEvent = dev.thural.quietspace.core.messaging.event.message.ChatEvent.builder()
                .chatId(chatId)
                .actorId(user1Id)
                .type(dev.thural.quietspace.core.messaging.event.EventType.LEFT_CHAT)
                .build();

        user1Session.send("/app/private/chat/leave", chatEvent);

        String event = notification.get(30, TimeUnit.SECONDS);
        assertThat(event).contains("LEFT_CHAT");
    }

    @Test
    void joinChat_shouldNotifyParticipants() throws Exception {
        StompSession user1Session = connectStomp(user1Jwt);
        StompSession user2Session = connectStomp(user2Jwt);

        CompletableFuture<String> notification = new CompletableFuture<>();

        Predicate<String> joinedMatcher = json -> json.contains("JOINED_CHAT") || json.contains("added");

        StompFrameHandler handler = new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String json = decodePayload(payload);
                if (joinedMatcher.test(json)) {
                    notification.complete(json);
                }
            }
        };

        subscribeWithReceipt(user1Session, "/public/chat", handler);
        subscribeWithReceipt(user2Session, "/public/chat", handler);

        var chatEvent = dev.thural.quietspace.core.messaging.event.message.ChatEvent.builder()
                .chatId(chatId)
                .actorId(user1Id)
                .recipientId(user2Id)
                .type(dev.thural.quietspace.core.messaging.event.EventType.JOINED_CHAT)
                .build();

        user1Session.send("/app/private/chat/join", chatEvent);

        String event = notification.get(30, TimeUnit.SECONDS);
        assertThat(event).contains("JOINED_CHAT");
    }
}