package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserFollowedEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new UserFollowedEvent();
        assertThat(event.getEventType()).isEqualTo("UserFollowed");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var followerId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        var event = new UserFollowedEvent(followerId, followedId);

        assertThat(event.getEventType()).isEqualTo("UserFollowed");
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getAggregateId()).isEqualTo(followedId);
        assertThat(event.getFollowerId()).isEqualTo(followerId);
        assertThat(event.getFollowedId()).isEqualTo(followedId);
    }
}