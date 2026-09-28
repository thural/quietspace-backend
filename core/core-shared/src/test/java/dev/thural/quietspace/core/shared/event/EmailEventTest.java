package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailEventTest {

    @Test
    void record_createsCompleteEvent() {
        var event = new EmailEvent("test@example.com", "Hello", "template", Map.of("key", "value"));

        assertThat(event.to()).isEqualTo("test@example.com");
        assertThat(event.subject()).isEqualTo("Hello");
        assertThat(event.templateName()).isEqualTo("template");
        assertThat(event.variables()).containsEntry("key", "value");
    }
}