package dev.thural.quietspace.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequest {

    @NotNull(message = "user id can not be null")
    private UUID userId;

    @NotNull(message = "post id can not be null")
    private UUID postId;

    @NotNull(message = "vote label can not be null")
    private String option;

    private UUID optionId;
}