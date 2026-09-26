package dev.thural.quietspace.domain.comment.adapter;

import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentNotificationAdapterTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentNotificationAdapter adapter;

    @Test
    void findCommentOwnerId_givenExistingComment_shouldReturnOwnerId() {
        UUID commentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Comment comment = Comment.builder().id(commentId).userId(ownerId).text("hi").build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThat(adapter.findCommentOwnerId(commentId)).isEqualTo(ownerId);
    }

    @Test
    void findCommentOwnerId_givenMissingComment_shouldThrow() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findCommentOwnerId(commentId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
