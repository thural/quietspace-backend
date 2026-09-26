package dev.thural.quietspace.domain.comment.api;

import dev.thural.quietspace.domain.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Default {@link CommentCommandPort} implementation backed by {@link CommentRepository}.
 */
@Component
@RequiredArgsConstructor
public class CommentCommandAdapter implements CommentCommandPort {

    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void deleteAllByPostId(UUID postId) {
        commentRepository.deleteAllByPostId(postId);
    }
}
