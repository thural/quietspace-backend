package dev.thural.quietspace.core.shared.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventSerializer eventSerializer;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TransactionalEventPublisher publisher;

    @Test
    void publish_savesToOutboxAndFlushes() {
        ReflectionTestUtils.setField(publisher, "entityManager", entityManager);
        
        var event = new UserRegisteredEvent(UUID.randomUUID(), "user", "u@x.com");
        when(eventSerializer.serialize(event)).thenReturn("{\"eventType\":\"UserRegistered\"}");

        publisher.publish(event);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(entityManager).flush();
    }
}