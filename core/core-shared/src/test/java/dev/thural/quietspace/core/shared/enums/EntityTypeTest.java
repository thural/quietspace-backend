package dev.thural.quietspace.core.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityTypeTest {

    @Test
    void values_containsExpectedTypes() {
        assertThat(EntityType.values()).containsExactlyInAnyOrder(
                EntityType.POST,
                EntityType.COMMENT,
                EntityType.MESSAGE,
                EntityType.USER
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(EntityType.valueOf("POST")).isEqualTo(EntityType.POST);
    }
}