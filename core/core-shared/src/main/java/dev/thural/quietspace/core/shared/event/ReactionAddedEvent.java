package dev.thural.quietspace.core.shared.event;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
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