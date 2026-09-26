package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrivacyChangedEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new UserPrivacyChangedEvent();
        assertThat(event.getEventType()).isEqualTo("UserPrivacyChanged");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var userId = UUID.randomUUID();
        var event = new UserPrivacyChangedEvent(userId, true);

        assertThat(event.getEventType()).isEqualTo("UserPrivacyChanged");
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getAggregateId()).isEqualTo(userId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.isPrivate()).isTrue();
    }
}
