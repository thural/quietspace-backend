package dev.thural.quietspace.shared.service.impl;

import dev.thural.quietspace.shared.event.EmailEvent;
import dev.thural.quietspace.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "email.queue")
    public void handleEmailEvent(EmailEvent event) {
        log.info("consuming email event for {}", event.to());
        emailService.sendHtmlEmail(
                event.to(),
                event.subject(),
                event.templateName(),
                event.variables()
        );
    }
}
