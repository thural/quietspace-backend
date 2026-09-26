package dev.thural.quietspace.core.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserUnfollowedEvent extends DomainEvent {

    private UUID followerId;
    private UUID followedId;

    public UserUnfollowedEvent() {
        setEventType("UserUnfollowed");
    }

    public UserUnfollowedEvent(UUID followerId, UUID followedId) {
        this();
        setAggregateType("User");
        setAggregateId(followedId);
        this.followerId = followerId;
        this.followedId = followedId;
    }
}
