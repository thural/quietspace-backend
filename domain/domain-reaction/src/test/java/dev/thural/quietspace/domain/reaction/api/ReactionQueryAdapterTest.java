package dev.thural.quietspace.domain.reaction.api;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.reaction.Reaction;
import dev.thural.quietspace.domain.reaction.ReactionRepository;
import dev.thural.quietspace.domain.reaction.api.dto.ReactionSummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionQueryAdapterTest {

    @Mock
    private ReactionRepository reactionRepository;

    @InjectMocks
    private ReactionQueryAdapter adapter;

    private static Reaction reaction(UUID id, UUID contentId) {
        return Reaction.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .username("actor")
                .contentId(contentId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();
    }

    @Test
    void getReactionSummary_givenExistingReaction_shouldReturnSnapshot() {
        UUID id = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(reactionRepository.findById(id)).thenReturn(Optional.of(reaction(id, contentId)));

        Optional<ReactionSummaryDTO> result = adapter.getReactionSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().contentId()).isEqualTo(contentId);
        assertThat(result.get().reactionType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    void getReactionSummary_givenMissingReaction_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(reactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.getReactionSummary(id)).isEmpty();
    }

    @Test
    void getReactionsSummary_givenIds_shouldReturnKeyedMap() {
        UUID id1 = UUID.randomUUID();
        when(reactionRepository.findAllById(Set.of(id1)))
                .thenReturn(List.of(reaction(id1, UUID.randomUUID())));

        Map<UUID, ReactionSummaryDTO> result = adapter.getReactionsSummary(Set.of(id1));

        assertThat(result).hasSize(1);
        assertThat(result.get(id1).username()).isEqualTo("actor");
    }

    @Test
    void getReactionsSummary_givenEmptySet_shouldReturnEmptyMapWithoutQuery() {
        assertThat(adapter.getReactionsSummary(Set.of())).isEmpty();
    }

    @Test
    void countReactions_shouldDelegateToRepository() {
        UUID contentId = UUID.randomUUID();
        when(reactionRepository.countByContentIdAndReactionType(contentId, ReactionType.LIKE))
                .thenReturn(3);

        assertThat(adapter.countReactions(contentId, ReactionType.LIKE)).isEqualTo(3);
    }

    @Test
    void countReactions_givenNullCount_shouldReturnZero() {
        UUID contentId = UUID.randomUUID();
        when(reactionRepository.countByContentIdAndReactionType(contentId, ReactionType.DISLIKE))
                .thenReturn(null);

        assertThat(adapter.countReactions(contentId, ReactionType.DISLIKE)).isZero();
    }
}
