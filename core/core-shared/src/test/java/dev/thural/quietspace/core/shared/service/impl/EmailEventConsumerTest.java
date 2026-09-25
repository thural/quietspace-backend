package dev.thural.quietspace.core.shared.service.impl;

import dev.thural.quietspace.core.shared.event.EmailEvent;
import dev.thural.quietspace.core.shared.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailEventConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailEventConsumer consumer;

    @Test
    void handleEmailEvent_delegatesToEmailService() {
        var event = new EmailEvent("test@example.com", "test", "template", Map.of("key", "value"));

        consumer.handleEmailEvent(event);

        verify(emailService).sendHtmlEmail("test@example.com", "test", "template", Map.of("key", "value"));
    }
}