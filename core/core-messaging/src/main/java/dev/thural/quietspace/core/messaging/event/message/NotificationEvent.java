package dev.thural.quietspace.websocket.event.message;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


@Getter
@Setter
@SuperBuilder
public class NotificationEvent extends BaseEvent {
    
    private UUID actorId;
    private UUID notificationId;
    private UUID recipientId;

}
