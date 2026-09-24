package dev.thural.quietspace.comment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
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
public class CommentResponse extends BaseResponse {

    private UUID userId;
    private UUID postId;
    private String text;
    private UUID parentId;
    private String username;
    private Integer likeCount;
    private Integer replyCount;
    private ReactionResponse userReaction;

}
