package dev.thural.quietspace.shared.messaging;

import dev.thural.quietspace.shared.event.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchanges.domain-events:domain.events}")
    private String domainEventsExchange;

    public void publish(OutboxEvent outboxEvent) {
        String routingKey = outboxEvent.getAggregateType() + "." + outboxEvent.getEventType();
        rabbitTemplate.convertAndSend(domainEventsExchange, routingKey, outboxEvent.getPayload());
        log.debug("Published outbox event {} to exchange {} with routing key {}", 
                outboxEvent.getId(), domainEventsExchange, routingKey);
    }
}