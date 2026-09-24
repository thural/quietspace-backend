package dev.thural.quietspace.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatRequest {

    @NotNull
    private Boolean isGroupChat;

    @NotNull
    private UUID recipientId;

    @NotNull
    private String text;
    private List<UUID> userIds;

}