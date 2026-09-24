package dev.thural.quietspace.shared.service.impl;

import dev.thural.quietspace.shared.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(EmailEvent event) {
        log.info("publishing email event for {}", event.to());
        rabbitTemplate.convertAndSend(
                "email.exchange",
                "email.send",
                event
        );
    }
}
