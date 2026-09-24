package dev.thural.quietspace.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserRegisteredEvent extends DomainEvent {

    private String username;
    private String email;

    public UserRegisteredEvent() {
        setEventType("UserRegistered");
    }

    public UserRegisteredEvent(UUID userId, String username, String email) {
        this();
        setAggregateType("User");
        setAggregateId(userId);
        this.username = username;
        this.email = email;
    }
}