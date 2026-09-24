package dev.thural.quietspace.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PostCreatedEvent extends DomainEvent {

    private String title;
    private String text;
    private UUID authorId;

    public PostCreatedEvent() {
        setEventType("PostCreated");
    }

    public PostCreatedEvent(UUID postId, UUID authorId, String title, String text) {
        this();
        setAggregateType("Post");
        setAggregateId(postId);
        this.authorId = authorId;
        this.title = title;
        this.text = text;
    }
}