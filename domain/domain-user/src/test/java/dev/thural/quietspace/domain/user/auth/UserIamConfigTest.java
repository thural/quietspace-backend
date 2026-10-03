package dev.thural.quietspace.domain.user.auth;

import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIamConfigTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserIamConfig beans;

    @Test
    void userDetailsService_givenEmail_shouldLoadByEmail() {
        User user = User.builder().id(UUID.randomUUID()).username("u").build();
        when(userRepository.findUserEntityByEmail("u@x.com")).thenReturn(Optional.of(user));

        assertThat(beans.userDetailsService().loadUserByUsername("u@x.com").getUsername())
                .isEqualTo("u");
    }

    @Test
    void userDetailsService_givenUsername_shouldFallbackToUsername() {
        User user = User.builder().id(UUID.randomUUID()).username("u").build();
        when(userRepository.findUserEntityByEmail("u")).thenReturn(Optional.empty());
        when(userRepository.findUserByUsername("u")).thenReturn(Optional.of(user));

        assertThat(beans.userDetailsService().loadUserByUsername("u").getUsername())
                .isEqualTo("u");
    }

    @Test
    void userDetailsService_givenUnknown_shouldThrow() {
        when(userRepository.findUserEntityByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beans.userDetailsService().loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void authenticationProvider_shouldBuildWithPasswordEncoder() {
        var provider = beans.authenticationProvider(passwordEncoder);

        assertThat(provider).isNotNull();
    }
}
