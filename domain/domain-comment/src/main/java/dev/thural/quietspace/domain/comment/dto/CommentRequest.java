package dev.thural.quietspace.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {

    @NotNull(message = "user id can not be null")
    private UUID userId;

    @NotNull(message = "post id can not be null")
    private UUID postId;

    private UUID parentId;

    @NotBlank(message = "comment text can not be blank")
    @Size(min = 1, max = 255, message = "at least 1 and max 255 characters expected")
    private String text;

}
