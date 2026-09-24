package dev.thural.quietspace.domain.post.adapter;

import dev.thural.quietspace.domain.comment.port.CommentPostPort;
import dev.thural.quietspace.domain.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Post-domain implementation of the comment-owned {@link CommentPostPort}.
 */
@Component
@RequiredArgsConstructor
public class PostCommentAdapter implements CommentPostPort {

    private final PostRepository postRepository;

    @Override
    public boolean postExists(UUID postId) {
        return postRepository.existsById(postId);
    }
}
