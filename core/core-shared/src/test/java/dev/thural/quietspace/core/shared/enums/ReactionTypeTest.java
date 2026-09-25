package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionTypeTest {

    @Test
    void values_containsExpectedTypes() {
        assertThat(ReactionType.values()).containsExactlyInAnyOrder(
                ReactionType.LIKE,
                ReactionType.DISLIKE
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(ReactionType.valueOf("LIKE")).isEqualTo(ReactionType.LIKE);
    }
}