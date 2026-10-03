package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.security.port.CurrentUserPort;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationAdapterTest {

    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserNotificationAdapter adapter;

    @Test
    void currentUserId_shouldReturnSignedUserId() {
        UUID id = UUID.randomUUID();
        when(currentUserPort.currentUserId()).thenReturn(id);

        assertThat(adapter.currentUserId()).isEqualTo(id);
    }

    @Test
    void findUsernameById_givenExistingUser_shouldReturnUsername() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id))
                .thenReturn(Optional.of(User.builder().id(id).username("owner").build()));

        assertThat(adapter.findUsernameById(id)).isEqualTo("owner");
    }

    @Test
    void findUsernameById_givenMissingUser_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findUsernameById(id))
                .isInstanceOf(UserNotFoundException.class);
    }
}
