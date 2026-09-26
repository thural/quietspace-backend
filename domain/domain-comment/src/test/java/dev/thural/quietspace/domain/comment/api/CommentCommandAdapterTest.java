package dev.thural.quietspace.domain.comment.api;

import dev.thural.quietspace.domain.comment.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentCommandAdapterTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentCommandAdapter adapter;

    @Test
    void deleteAllByPostId_shouldDelegateToRepository() {
        UUID postId = UUID.randomUUID();

        adapter.deleteAllByPostId(postId);

        verify(commentRepository).deleteAllByPostId(postId);
    }
}
