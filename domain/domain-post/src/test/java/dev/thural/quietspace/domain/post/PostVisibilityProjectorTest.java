package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.core.shared.event.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostVisibilityProjectorTest {

    @Mock
    private PostAuthorVisibilityRepository visibilityRepository;
    @Mock
    private ViewerAuthorAccessRepository accessRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private PostVisibilityProjector projector;

    @Test
    void onUserRegistered_shouldInsertDefaultPublicRow() {
        UUID userId = UUID.randomUUID();
        var event = new UserRegisteredEvent(userId, "u", "u@test.com");
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(visibilityRepository.findById(userId)).thenReturn(Optional.empty());

        projector.onUserRegistered(event);

        var captor = ArgumentCaptor.forClass(PostAuthorVisibility.class);
        verify(visibilityRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(userId);
        assertThat(captor.getValue().getIsPrivate()).isFalse();
    }

    @Test
    void onUserPrivacyChanged_shouldUpsertRow() {
        UUID userId = UUID.randomUUID();
        var event = new UserPrivacyChangedEvent(userId, true);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(visibilityRepository.findById(userId)).thenReturn(Optional.empty());

        projector.onUserPrivacyChanged(event);

        var captor = ArgumentCaptor.forClass(PostAuthorVisibility.class);
        verify(visibilityRepository).save(captor.capture());
        assertThat(captor.getValue().getIsPrivate()).isTrue();
    }

    @Test
    void onUserFollowed_shouldInsertAccessEdge() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        var event = new UserFollowedEvent(followerId, followedId);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(accessRepository.existsByViewerIdAndAuthorId(followerId, followedId)).thenReturn(false);

        projector.onUserFollowed(event);

        verify(accessRepository).save(any(ViewerAuthorAccess.class));
    }

    @Test
    void onUserFollowed_givenExistingEdge_shouldNotDuplicate() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        var event = new UserFollowedEvent(followerId, followedId);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(accessRepository.existsByViewerIdAndAuthorId(followerId, followedId)).thenReturn(true);

        projector.onUserFollowed(event);

        verify(accessRepository, never()).save(any());
    }

    @Test
    void onUserUnfollowed_shouldDeleteAccessEdge() {
        UUID followerId = UUID.randomUUID();
        UUID followedId = UUID.randomUUID();
        var event = new UserUnfollowedEvent(followerId, followedId);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);

        projector.onUserUnfollowed(event);

        verify(accessRepository).deleteByViewerIdAndAuthorId(followerId, followedId);
    }

    @Test
    void onEvent_givenAlreadyProcessed_shouldSkip() {
        UUID userId = UUID.randomUUID();
        var event = new UserPrivacyChangedEvent(userId, true);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        projector.onUserPrivacyChanged(event);

        verify(visibilityRepository, never()).findById(any());
        verify(visibilityRepository, never()).save(any());
    }
}
