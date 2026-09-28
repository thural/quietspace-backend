package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Reaction extends BaseEntity {

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID userId;

    @NotNull
    private String username;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID contentId;

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