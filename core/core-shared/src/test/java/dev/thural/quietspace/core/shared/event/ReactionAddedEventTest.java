package dev.thural.quietspace.core.shared.event;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionAddedEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new ReactionAddedEvent();
        assertThat(event.getEventType()).isEqualTo("ReactionAdded");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var reactionId = UUID.randomUUID();
        var contentId = UUID.randomUUID();
        var actorId = UUID.randomUUID();
        var event = new ReactionAddedEvent(reactionId, contentId,
                EntityType.POST, ReactionType.LIKE, actorId);

        assertThat(event.getEventType()).isEqualTo("ReactionAdded");
        assertThat(event.getAggregateType()).isEqualTo("Reaction");
        assertThat(event.getAggregateId()).isEqualTo(reactionId);
        assertThat(event.getContentId()).isEqualTo(contentId);
        assertThat(event.getContentType()).isEqualTo(EntityType.POST);
        assertThat(event.getReactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(event.getActorId()).isEqualTo(actorId);
    }
}