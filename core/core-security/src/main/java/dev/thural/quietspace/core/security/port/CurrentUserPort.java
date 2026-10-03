package dev.thural.quietspace.core.security.port;

import java.util.UUID;

public interface CurrentUserPort {
    UUID currentUserId();
}