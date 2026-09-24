package dev.thural.quietspace.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserFollowedEvent extends DomainEvent {

    private UUID followerId;
    private UUID followedId;

    public UserFollowedEvent() {
        setEventType("UserFollowed");
    }

    public UserFollowedEvent(UUID followerId, UUID followedId) {
        this();
        setAggregateType("User");
        setAggregateId(followedId);
        this.followerId = followerId;
        this.followedId = followedId;
    }
}