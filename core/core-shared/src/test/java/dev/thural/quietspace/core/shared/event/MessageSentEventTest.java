package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSentEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new MessageSentEvent();
        assertThat(event.getEventType()).isEqualTo("MessageSent");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var messageId = UUID.randomUUID();
        var chatId = UUID.randomUUID();
        var senderId = UUID.randomUUID();
        var event = new MessageSentEvent(messageId, chatId, senderId, "hello");

        assertThat(event.getEventType()).isEqualTo("MessageSent");
        assertThat(event.getAggregateType()).isEqualTo("Message");
        assertThat(event.getAggregateId()).isEqualTo(messageId);
        assertThat(event.getChatId()).isEqualTo(chatId);
        assertThat(event.getSenderId()).isEqualTo(senderId);
        assertThat(event.getText()).isEqualTo("hello");
    }
}