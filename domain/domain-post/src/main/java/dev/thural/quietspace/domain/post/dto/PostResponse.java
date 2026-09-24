package dev.thural.quietspace.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
import dev.thural.quietspace.shared.model.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResponse extends BaseResponse {

    private String userId;
    private String username;
    private String title;
    private String text;
    private String parentId;
    private Boolean isRepost;
    private PollResponse poll;
    private PhotoResponse photo;
    private PostResponse repost;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private ReactionResponse userReaction;

}