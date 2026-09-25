package dev.thural.quietspace.core.shared.service.impl;

import dev.thural.quietspace.core.shared.event.EmailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EmailEventPublisher publisher;

    @Test
    void publish_sendsToExchange() {
        var event = new EmailEvent("test@example.com", "test", "template", Map.of());

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend("email.exchange", "email.send", event);
    }
}