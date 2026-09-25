package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegisteredEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new UserRegisteredEvent();
        assertThat(event.getEventType()).isEqualTo("UserRegistered");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var userId = UUID.randomUUID();
        var event = new UserRegisteredEvent(userId, "john", "john@example.com");

        assertThat(event.getEventType()).isEqualTo("UserRegistered");
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getAggregateId()).isEqualTo(userId);
        assertThat(event.getUsername()).isEqualTo("john");
        assertThat(event.getEmail()).isEqualTo("john@example.com");
    }
}