package dev.thural.quietspace.websocket.event.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.websocket.event.EventType;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Jacksonized
@MappedSuperclass
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseEvent {

    String message;
    Object eventBody;

    @NotNull
    EventType type;

}
