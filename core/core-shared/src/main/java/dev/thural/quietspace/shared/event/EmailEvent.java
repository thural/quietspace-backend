package dev.thural.quietspace.shared.event;

import java.io.Serializable;
import java.util.Map;

public record EmailEvent(
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables
) implements Serializable {}
