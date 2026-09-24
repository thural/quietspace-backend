package dev.thural.quietspace.reaction.dto;

import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

    @NotNull(message = "reaction user id can not be null")
    private UUID userId;

    @NotNull(message = "reaction content id can not be null")
    private UUID contentId;

    @NotNull(message = "reaction content type can not be null")
    private EntityType contentType;

    @NotNull(message = "reaction type can not be null")
    private ReactionType reactionType;

}