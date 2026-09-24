package dev.thural.quietspace.core.shared.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationProvider {
    UserDetails loadUserByUsername(String username);
}
