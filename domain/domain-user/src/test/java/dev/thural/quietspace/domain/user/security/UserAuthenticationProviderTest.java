package dev.thural.quietspace.domain.user.security;

import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAuthenticationProvider provider;

    @Test
    void loadUserByUsername_givenExistingUser_shouldReturnDetails() {
        User user = User.builder().id(UUID.randomUUID()).username("u").build();
        when(userRepository.findUserByUsername("u")).thenReturn(Optional.of(user));

        assertThat(provider.loadUserByUsername("u").getUsername()).isEqualTo("u");
    }

    @Test
    void loadUserByUsername_givenMissingUser_shouldThrow() {
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
