package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileAdapterTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileAdapter adapter;

    @Test
    void currentUserId_shouldReturnSignedUserId() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("u").build();
        when(userService.getSignedUser()).thenReturn(user);

        assertThat(adapter.currentUserId()).isEqualTo(id);
    }

    @Test
    void setProfilePhoto_givenExistingUser_shouldSetPhotoId() {
        UUID userId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        User user = User.builder().id(userId).username("u").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adapter.setProfilePhoto(userId, photoId);

        assertThat(user.getPhotoId()).isEqualTo(photoId);
    }

    @Test
    void setProfilePhoto_givenMissingUser_shouldDoNothing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        adapter.setProfilePhoto(userId, UUID.randomUUID());

        verify(userRepository).findById(userId);
    }
}
