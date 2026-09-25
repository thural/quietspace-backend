package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.shared.enums.StatusType;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketUserAdapterTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketUserAdapter adapter;

    @Test
    void currentUserId_shouldReturnSignedUserId() {
        UUID id = UUID.randomUUID();
        when(userService.getSignedUser()).thenReturn(User.builder().id(id).username("u").build());

        assertThat(adapter.currentUserId()).isEqualTo(id);
    }

    @Test
    void userIdForUsername_givenExistingUser_shouldReturnId() {
        UUID id = UUID.randomUUID();
        when(userRepository.findUserByUsername("u"))
                .thenReturn(Optional.of(User.builder().id(id).username("u").build()));

        assertThat(adapter.userIdForUsername("u")).isEqualTo(id);
    }

    @Test
    void userIdForUsername_givenMissingUser_shouldThrow() {
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.userIdForUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void userForId_givenExistingUser_shouldReturnDetails() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("u").build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(adapter.userForId(id).getUsername()).isEqualTo("u");
    }

    @Test
    void userForId_givenMissingUser_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.userForId(id))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void setOnlineStatus_shouldDelegateToUserService() {
        adapter.setOnlineStatus("u", StatusType.ONLINE);

        verify(userService).setOnlineStatus("u", StatusType.ONLINE);
    }
}
