package dev.thural.quietspace.core.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserPrivacyChangedEvent extends DomainEvent {

    private UUID userId;
    private boolean isPrivate;

    public UserPrivacyChangedEvent() {
        setEventType("UserPrivacyChanged");
    }

    public UserPrivacyChangedEvent(UUID userId, boolean isPrivate) {
        this();
        setAggregateType("User");
        setAggregateId(userId);
        this.userId = userId;
        this.isPrivate = isPrivate;
    }
}
