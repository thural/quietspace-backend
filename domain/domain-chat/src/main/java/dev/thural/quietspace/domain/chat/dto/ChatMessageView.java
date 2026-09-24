package dev.thural.quietspace.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Chat-owned read view of a message.
 *
 * <p>Owned by domain-chat so chat responses never require a dependency on the
 * message module. Populated via {@code ChatMessagePort}.</p>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageView {

    private UUID id;
    private UUID chatId;
    private UUID senderId;
    private UUID recipientId;
    private String text;
    private Boolean isSeen;
    private UUID photoId;

}
