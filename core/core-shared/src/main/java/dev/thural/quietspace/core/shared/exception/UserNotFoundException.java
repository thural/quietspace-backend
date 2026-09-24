package dev.thural.quietspace.core.shared.exception;

import jakarta.persistence.EntityNotFoundException;

public class UserNotFoundException extends EntityNotFoundException {
    public UserNotFoundException() {
        super("user not found");
    }
    public UserNotFoundException(String message) {
        super(message);
    }
}
