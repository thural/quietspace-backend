package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.shared.exception.UnauthenticatedException;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserAdapter adapter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserId_givenAuthenticatedUser_shouldResolveIdViaUsername() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList()));
        when(userRepository.findUserByUsername("user"))
                .thenReturn(Optional.of(User.builder().id(id).username("user").build()));

        assertThat(adapter.currentUserId()).isEqualTo(id);
    }

    @Test
    void currentUserId_givenNoAuthentication_shouldThrowUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> adapter.currentUserId())
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void currentUserId_givenAnonymousPrincipal_shouldThrowUnauthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList()));

        assertThatThrownBy(() -> adapter.currentUserId())
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void currentUserId_givenUnknownUser_shouldThrowUserNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null, Collections.emptyList()));
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.currentUserId())
                .isInstanceOf(UserNotFoundException.class);
    }
}
