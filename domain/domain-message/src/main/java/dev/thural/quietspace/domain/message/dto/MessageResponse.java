package dev.thural.quietspace.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.shared.model.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse extends BaseResponse {

    private UUID chatId;
    private String text;
    private PhotoResponse photo;
    private Boolean isSeen;
    private UUID senderId;
    private UUID recipientId;
    private String senderName;

}