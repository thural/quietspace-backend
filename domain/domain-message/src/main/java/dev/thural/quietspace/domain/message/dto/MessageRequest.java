package dev.thural.quietspace.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    @NotNull(message = "chat id can not be null")
    private UUID chatId;

    @NotNull(message = "sender id can not be null")
    private UUID senderId;

    @NotNull
    private UUID recipientId;

    @NotNull(message = "text content can not be null")
    @NotBlank(message = "text content can not be blank")
    @Size(min = 1, max = 1000, message = "at lest 1 and max 1000 characters expected")
    private String text;

    @JsonIgnore
    private MultipartFile photoData;
}
