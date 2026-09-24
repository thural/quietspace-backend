package dev.thural.quietspace.shared.exception;

import org.springframework.messaging.MessagingException;

public class CustomMessagingException extends MessagingException {

    public CustomMessagingException(String message) {
        super("messaging error occurred: ".concat(message));
    }
    
}
