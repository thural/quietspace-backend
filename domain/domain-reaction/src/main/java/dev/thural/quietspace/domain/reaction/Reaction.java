package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.core.shared.entity.BaseEntity;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Reaction extends BaseEntity {

    @NotNull    private UUID userId;

    @NotNull
    private String username;

    @NotNull    private UUID contentId;

    @Enumerated(EnumType.STRING)
    private EntityType contentType;

    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    // Factory method with validation
    public static Reaction create(UUID userId, String username, UUID contentId, EntityType contentType, ReactionType reactionType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (contentId == null) {
            throw new IllegalArgumentException("Content ID is required");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("Content type is required");
        }
        if (reactionType == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }
        
        Reaction reaction = new Reaction();
        reaction.setUserId(userId);
        reaction.setUsername(username);
        reaction.setContentId(contentId);
        reaction.setContentType(contentType);
        reaction.setReactionType(reactionType);
        return reaction;
    }
}