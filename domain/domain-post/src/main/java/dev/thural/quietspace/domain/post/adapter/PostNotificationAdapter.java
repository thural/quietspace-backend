package dev.thural.quietspace.domain.post.adapter;

import dev.thural.quietspace.domain.notification.port.NotificationPostPort;
import dev.thural.quietspace.domain.post.Post;
import dev.thural.quietspace.domain.post.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Post-domain implementation of the notification-owned {@link NotificationPostPort}.
 */
@Component
@RequiredArgsConstructor
public class PostNotificationAdapter implements NotificationPostPort {

    private final PostRepository postRepository;

    @Override
    public UUID findPostOwnerId(UUID postId) {
        return postRepository.findById(postId)
                .map(Post::getAuthorId)
                .orElseThrow(EntityNotFoundException::new);
    }
}
