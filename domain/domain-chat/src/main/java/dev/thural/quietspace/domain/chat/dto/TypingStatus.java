package dev.thural.quietspace.chat.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingStatus {

    private UUID chatId;
    private UUID userId;
    private Boolean isTyping;

}
