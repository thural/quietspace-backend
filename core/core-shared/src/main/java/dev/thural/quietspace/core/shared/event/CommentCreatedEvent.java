package dev.thural.quietspace.core.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CommentCreatedEvent extends DomainEvent {

    private String text;
    private UUID postId;
    private UUID authorId;

    public CommentCreatedEvent() {
        setEventType("CommentCreated");
    }

    public CommentCreatedEvent(UUID commentId, UUID postId, UUID authorId, String text) {
        this();
        setAggregateType("Comment");
        setAggregateId(commentId);
        this.postId = postId;
        this.authorId = authorId;
        this.text = text;
    }
}