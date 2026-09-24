package dev.thural.quietspace.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationProvider {
    UserDetails loadUserByUsername(String username);
}