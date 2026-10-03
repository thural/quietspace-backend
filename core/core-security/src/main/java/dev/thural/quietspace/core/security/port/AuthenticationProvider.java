package dev.thural.quietspace.core.security.port;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationProvider {
    UserDetails loadUserByUsername(String username);
}