package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void defaultValues_areSetOnConstruction() {
        var event = new TestEvent();

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAggregateType()).isNull();
        assertThat(event.getAggregateId()).isNull();
        assertThat(event.getEventType()).isNull();
    }

    @Test
    void setters_work() {
        var event = new TestEvent();
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now();

        event.setEventId(id);
        event.setTimestamp(now);
        event.setAggregateType("Test");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("TestEvent");

        assertThat(event.getEventId()).isEqualTo(id);
        assertThat(event.getTimestamp()).isEqualTo(now);
        assertThat(event.getAggregateType()).isEqualTo("Test");
        assertThat(event.getEventType()).isEqualTo("TestEvent");
    }

    private static final class TestEvent extends DomainEvent {}
}