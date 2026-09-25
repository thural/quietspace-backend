package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentCreatedEventTest {

    @Test
    void defaultConstructor_setsEventType() {
        var event = new CommentCreatedEvent();
        assertThat(event.getEventType()).isEqualTo("CommentCreated");
    }

    @Test
    void fullConstructor_setsAllFields() {
        var commentId = UUID.randomUUID();
        var postId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var event = new CommentCreatedEvent(commentId, postId, authorId, "nice post");

        assertThat(event.getEventType()).isEqualTo("CommentCreated");
        assertThat(event.getAggregateType()).isEqualTo("Comment");
        assertThat(event.getAggregateId()).isEqualTo(commentId);
        assertThat(event.getPostId()).isEqualTo(postId);
        assertThat(event.getAuthorId()).isEqualTo(authorId);
        assertThat(event.getText()).isEqualTo("nice post");
    }
}