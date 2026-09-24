package dev.thural.quietspace.domain.notification;

import lombok.Getter;

@Getter
public enum NotificationType {
    FOLLOW_REQUEST,
    POST_REACTION,
    COMMENT,
    MENTION,
    COMMENT_REACTION,
    COMMENT_REPLY,
    REPOST
}