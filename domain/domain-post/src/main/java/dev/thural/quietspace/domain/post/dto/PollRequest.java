package dev.thural.quietspace.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollRequest {

    private OffsetDateTime dueDate;

    @NotNull(message = "poll options can not be null")
    private List<String> options;

}