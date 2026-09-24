package dev.thural.quietspace.reaction;

import dev.thural.quietspace.reaction.dto.ReactionRequest;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface ReactionService {

    void handleReaction(ReactionRequest reaction);

    Optional<ReactionResponse> getUserReactionByContentId(UUID contentId);

    Page<ReactionResponse> getReactionsByContentIdAndReactionType(UUID contentId, ReactionType reactionType, Integer pageNumber, Integer pageSize);

    Integer countByContentIdAndReactionType(UUID contentId, ReactionType reactionType);

    Page<ReactionResponse> getReactionsByContentIdAndContentType(UUID contentId, EntityType type, Integer pageNumber, Integer pageSize);

    Page<ReactionResponse> getReactionsByUserIdAndContentType(UUID userId, EntityType contentType, Integer pageNumber, Integer pageSize);

    void addReaction(ReactionRequest reaction);

    void removeReaction(UUID reactionId);

}
