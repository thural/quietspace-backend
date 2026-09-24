package dev.thural.quietspace.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MessageSentEvent extends DomainEvent {

    private UUID chatId;
    private UUID senderId;
    private String text;

    public MessageSentEvent() {
        setEventType("MessageSent");
    }

    public MessageSentEvent(UUID messageId, UUID chatId, UUID senderId, String text) {
        this();
        setAggregateType("Message");
        setAggregateId(messageId);
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
    }
}