package dev.thural.quietspace.core.messaging.event.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.core.messaging.event.EventType;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
