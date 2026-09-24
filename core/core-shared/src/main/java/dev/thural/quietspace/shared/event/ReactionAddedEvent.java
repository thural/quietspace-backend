package dev.thural.quietspace.shared.event;

import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReactionAddedEvent extends DomainEvent {

    private UUID contentId;
    private EntityType contentType;
    private ReactionType reactionType;
    private UUID actorId;

    public ReactionAddedEvent() {
        setEventType("ReactionAdded");
    }

    public ReactionAddedEvent(UUID reactionId, UUID contentId, EntityType contentType, ReactionType reactionType, UUID actorId) {
        this();
        setAggregateType("Reaction");
        setAggregateId(reactionId);
        this.contentId = contentId;
        this.contentType = contentType;
        this.reactionType = reactionType;
        this.actorId = actorId;
    }
}