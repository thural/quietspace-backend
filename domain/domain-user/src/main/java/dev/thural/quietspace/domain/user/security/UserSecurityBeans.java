package dev.thural.quietspace.domain.user.security;

import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * User-bound security beans.
 *
 * <p>Lives in domain-user (the provider) so core-security stays free of
 * domain dependencies. Only Spring types cross the module boundary.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserSecurityBeans {

    private final UserRepository userRepository;

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return username -> {
            log.debug("loading user by identifier");
            return userRepository.findUserEntityByEmail(username).orElseGet(
                    () -> userRepository.findUserByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"))
            );
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
