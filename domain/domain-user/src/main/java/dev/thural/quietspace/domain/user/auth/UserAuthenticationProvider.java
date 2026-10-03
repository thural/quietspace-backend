package dev.thural.quietspace.domain.user.auth;

import dev.thural.quietspace.core.security.port.AuthenticationProvider;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found with username: " + username));
    }
}