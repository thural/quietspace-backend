package dev.thural.quietspace.shared.service;

import dev.thural.quietspace.shared.exception.UserNotFoundException;
import dev.thural.quietspace.shared.service.impl.CommonServiceImpl;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CommonServiceImpl commonService;

    @Test
    void getSignedUser_whenAuthenticated_shouldReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("testuser").build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));

        User result = commonService.getSignedUser();

        assertThat(result).isEqualTo(user);
        verify(userRepository).findUserByUsername("testuser");
    }

    @Test
    void getSignedUser_whenUserNotFound_shouldThrow() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("unknown");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commonService.getSignedUser())
                .isInstanceOf(UserNotFoundException.class);
    }
}
