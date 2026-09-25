package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusTypeTest {

    @Test
    void values_containsExpectedStatuses() {
        assertThat(StatusType.values()).containsExactlyInAnyOrder(
                StatusType.ONLINE,
                StatusType.OFFLINE
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(StatusType.valueOf("ONLINE")).isEqualTo(StatusType.ONLINE);
    }
}