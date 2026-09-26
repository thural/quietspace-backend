package dev.thural.quietspace.domain.reaction.api;

import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.reaction.Reaction;
import dev.thural.quietspace.domain.reaction.ReactionRepository;
import dev.thural.quietspace.domain.reaction.api.dto.ReactionSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link ReactionQueryPort} implementation backed by {@link ReactionRepository}.
 */
@Component
@RequiredArgsConstructor
public class ReactionQueryAdapter implements ReactionQueryPort {

    private final ReactionRepository reactionRepository;

    @Override
    public Optional<ReactionSummaryDTO> getReactionSummary(UUID reactionId) {
        return reactionRepository.findById(reactionId).map(this::toSummary);
    }

    @Override
    public Map<UUID, ReactionSummaryDTO> getReactionsSummary(Set<UUID> reactionIds) {
        if (reactionIds == null || reactionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return reactionRepository.findAllById(reactionIds).stream()
                .collect(Collectors.toMap(Reaction::getId, this::toSummary));
    }

    @Override
    public long countReactions(UUID contentId, ReactionType reactionType) {
        Integer count = reactionRepository.countByContentIdAndReactionType(contentId, reactionType);
        return count != null ? count : 0;
    }

    private ReactionSummaryDTO toSummary(Reaction reaction) {
        return new ReactionSummaryDTO(
                reaction.getId(),
                reaction.getUserId(),
                reaction.getUsername(),
                reaction.getContentId(),
                reaction.getContentType(),
                reaction.getReactionType()
        );
    }
}
