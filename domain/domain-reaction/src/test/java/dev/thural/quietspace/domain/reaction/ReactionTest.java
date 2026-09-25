package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactionTest {

    @Test
    void create_givenValidParams_shouldBuildReaction() {
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        Reaction reaction = Reaction.create(userId, "user", contentId, EntityType.POST, ReactionType.LIKE);

        assertThat(reaction.getUserId()).isEqualTo(userId);
        assertThat(reaction.getContentId()).isEqualTo(contentId);
        assertThat(reaction.getContentType()).isEqualTo(EntityType.POST);
        assertThat(reaction.getReactionType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    void create_givenNullUserId_shouldThrow() {
        assertThatThrownBy(() -> Reaction.create(null, "user", UUID.randomUUID(),
                EntityType.POST, ReactionType.LIKE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_givenBlankUsername_shouldThrow() {
        assertThatThrownBy(() -> Reaction.create(UUID.randomUUID(), "  ", UUID.randomUUID(),
                EntityType.POST, ReactionType.LIKE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_givenNullContentId_shouldThrow() {
        assertThatThrownBy(() -> Reaction.create(UUID.randomUUID(), "user", null,
                EntityType.POST, ReactionType.LIKE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_givenNullContentType_shouldThrow() {
        assertThatThrownBy(() -> Reaction.create(UUID.randomUUID(), "user", UUID.randomUUID(),
                null, ReactionType.LIKE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_givenNullReactionType_shouldThrow() {
        assertThatThrownBy(() -> Reaction.create(UUID.randomUUID(), "user", UUID.randomUUID(),
                EntityType.POST, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
