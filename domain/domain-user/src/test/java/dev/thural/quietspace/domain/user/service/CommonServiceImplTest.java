package dev.thural.quietspace.domain.user.service;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommonServiceImpl commonService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSignedUser_givenAuthenticatedUser_shouldReturnUser() {
        User user = User.builder().id(UUID.randomUUID()).username("u").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", null, List.of()));
        when(userRepository.findUserByUsername("u")).thenReturn(Optional.of(user));

        assertThat(commonService.getSignedUser()).isEqualTo(user);
    }

    @Test
    void getSignedUser_givenNoAuthentication_shouldThrow() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> commonService.getSignedUser())
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getSignedUser_givenUnknownUser_shouldThrow() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost", null, List.of()));
        when(userRepository.findUserByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commonService.getSignedUser())
                .isInstanceOf(UserNotFoundException.class);
    }
}
