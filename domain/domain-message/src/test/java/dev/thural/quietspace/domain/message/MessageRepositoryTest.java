package dev.thural.quietspace.message;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.message.MessageRepository;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MessageRepositoryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MessageRepository messageRepository;

    private final User user1 = User.builder()
            .email("user1@email.com")
            .username("user1")
            .firstname("firstname1")
            .lastname("lastname1")
            .password("78921731")
            .accountLocked(false)
            .username("test user1")
            .role(Role.USER)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private final User user2 = User.builder()
            .email("user2@email.com")
            .username("user2")
            .firstname("firstname2")
            .lastname("lastname2")
            .password("78921732")
            .accountLocked(false)
            .username("test user2")
            .role(Role.USER)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private final Chat chat = Chat.builder()
            .users(List.of(user1))
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private final Message message = Message.builder()
            .text("sample text")
            .chat(chat)
            .sender(user1)
            .recipient(user2)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private Chat savedChat;
    private Message savedMessage;

    @BeforeEach
    void setUp() {
        this.userRepository.save(user1);
        this.userRepository.save(user2);
        this.savedChat = chatRepository.save(chat);
        this.savedMessage = messageRepository.save(message);
    }

    @AfterEach
    void tearDown() {
        this.userRepository.delete(user1);
        this.userRepository.delete(user2);
        this.chatRepository.delete(savedChat);
        this.messageRepository.delete(savedMessage);
    }

    @Test
    void findAllByChatId() {
        Page<Message> messagePage = messageRepository.findAllByChatId(chat.getId(), null);
        assertThat(messagePage.toList()).hasSize(1);
        assertThat(messagePage.toList().get(0).getText()).isEqualTo("sample text");
    }

    @Test
    void findFirstByChatOrderByCreateDateDesc() {
        Message latestMessage = messageRepository.findFirstByChatOrderByCreateDateDesc(chat).orElse(null);
        assertThat(latestMessage).isNotNull();
        assertThat(latestMessage.getText()).isEqualTo(savedMessage.getText());
    }

    @Test
    void findByMessageIdAndChatId() {
        Message foundMessage = messageRepository
                .findByMessageIdAndChatId(savedMessage.getId(), savedChat.getId()).orElse(null);
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getText()).isEqualTo("sample text");
    }

    @Test
    void findByMessageIdAndChatId_givenNonExistent_shouldReturnEmpty() {
        Message foundMessage = messageRepository
                .findByMessageIdAndChatId(UUID.randomUUID(), savedChat.getId()).orElse(null);
        assertThat(foundMessage).isNull();
    }
}