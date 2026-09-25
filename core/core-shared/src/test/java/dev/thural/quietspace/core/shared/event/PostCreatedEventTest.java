package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostCreatedEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new PostCreatedEvent();
        assertThat(event.getEventType()).isEqualTo("PostCreated");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var postId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var event = new PostCreatedEvent(postId, authorId, "title", "body");

        assertThat(event.getEventType()).isEqualTo("PostCreated");
        assertThat(event.getAggregateType()).isEqualTo("Post");
        assertThat(event.getAggregateId()).isEqualTo(postId);
        assertThat(event.getAuthorId()).isEqualTo(authorId);
        assertThat(event.getTitle()).isEqualTo("title");
        assertThat(event.getText()).isEqualTo("body");
    }
}