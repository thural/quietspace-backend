package dev.thural.quietspace.reaction;
import dev.thural.quietspace.shared.entity.BaseEntity;

import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
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
@AllArgsConstructor
public class Reaction extends BaseEntity {

    @NotNull
    private UUID userId;

    @NotNull
    private String username;

    @NotNull
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    private EntityType contentType;

    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

}
