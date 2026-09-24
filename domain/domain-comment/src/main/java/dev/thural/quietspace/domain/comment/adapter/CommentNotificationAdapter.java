package dev.thural.quietspace.domain.comment.adapter;

import dev.thural.quietspace.domain.comment.CommentRepository;
import dev.thural.quietspace.domain.notification.port.NotificationCommentPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Comment-domain implementation of the notification-owned {@link NotificationCommentPort}.
 */
@Component
@RequiredArgsConstructor
public class CommentNotificationAdapter implements NotificationCommentPort {

    private final CommentRepository commentRepository;

    @Override
    public UUID findCommentOwnerId(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(comment -> comment.getUser().getId())
                .orElseThrow(EntityNotFoundException::new);
    }
}
