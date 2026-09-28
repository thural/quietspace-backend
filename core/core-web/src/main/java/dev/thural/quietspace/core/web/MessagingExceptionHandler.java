package dev.thural.quietspace.core.web;

import dev.thural.quietspace.core.shared.exception.CustomMessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MessagingExceptionHandler {

    @MessageExceptionHandler(CustomMessagingException.class)
    @SendTo("/error")
    public String handleMyCustomException(CustomMessagingException e) {
        return e.getMessage();
    }

}
