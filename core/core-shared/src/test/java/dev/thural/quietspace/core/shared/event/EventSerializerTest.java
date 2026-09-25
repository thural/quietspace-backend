package dev.thural.quietspace.core.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventSerializerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final EventSerializer serializer = new EventSerializer(mapper);

    @Test
    void serialize_thenDeserialize_preservesEvent() {
        var original = new UserRegisteredEvent(UUID.randomUUID(), "john", "john@example.com");

        String json = serializer.serialize(original);
        var deserialized = serializer.deserialize(json, UserRegisteredEvent.class);

        assertThat(deserialized.getEventId()).isEqualTo(original.getEventId());
        assertThat(deserialized.getUsername()).isEqualTo("john");
        assertThat(deserialized.getEmail()).isEqualTo("john@example.com");
        assertThat(deserialized.getEventType()).isEqualTo("UserRegistered");
    }

    @Test
    void deserialize_invalidJson_throwsIllegalStateException() {
        assertThatThrownBy(() -> serializer.deserialize("not json", UserRegisteredEvent.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    @Test
    void serialize_null_returnsNullString() {
        String json = serializer.serialize(null);
        assertThat(json).isEqualTo("null");
    }
}