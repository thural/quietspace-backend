package dev.thural.quietspace.websocket.event.message;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Jacksonized
public class ChatEvent extends BaseEvent {

    private UUID chatId;
    private UUID actorId;
    private UUID messageId;
    private UUID recipientId;

}
