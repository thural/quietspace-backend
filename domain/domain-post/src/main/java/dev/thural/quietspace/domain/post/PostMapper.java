package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.domain.photo.dto.PhotoResponse;
import dev.thural.quietspace.domain.post.dto.*;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.thural.quietspace.core.shared.enums.ReactionType.DISLIKE;
import static dev.thural.quietspace.core.shared.enums.ReactionType.LIKE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostMapper {

    private final ReactionService reactionService;
    private final PostRepository postRepository;
    private final PhotoService photoService;
    private final UserService userService;

    public Post postRequestToEntity(PostRequest postRequest) {
        Post post = Post.builder()
                .user(getLoggedUser())
                .title(postRequest.getTitle())
                .text(postRequest.getText())
                .build();

        if (postRequest.getPoll() == null) return post;

        PollRequest pollRequest = postRequest.getPoll();

        Poll newPoll = Poll.builder()
                .post(post)
                .dueDate(pollRequest.getDueDate())
                .build();

        List<PollOption> options = pollRequest.getOptions().stream()
                .<PollOption>map(option -> PollOption.builder()
                        .label(option)
                        .poll(newPoll)
                        .votes(new HashSet<>())
                        .build())
                .toList();

        newPoll.setOptions(options);
        post.setPoll(newPoll);

        return post;
    }

    public PostResponse postEntityToResponse(Post entity) {

        boolean isRepost = entity.getRepostId() != null;
        Post post = isRepost ? postRepository.findById(UUID.fromString(entity.getRepostId()))
                .orElse(null) : entity;

        if (post == null) return null;

        Integer commentCount = post.getComments() != null ? post.getComments().size() : 0;
        Integer likeCount = reactionService.countByContentIdAndReactionType(post.getId(), LIKE);
        Integer dislikeCount = reactionService.countByContentIdAndReactionType(post.getId(), DISLIKE);
        ReactionResponse userReaction = reactionService.getUserReactionByContentId(post.getId())
                .orElse(null);

        PhotoResponse postPhoto = post.getPhotoId() == null ?
                null : photoService.getPhotoById(post.getPhotoId());

        PostResponse postResponse = PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .text(post.getText())
                .photo(postPhoto)
                .commentCount(commentCount)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .userId(post.getUser().getId().toString())
                .username(post.getUser().getUsername())
                .userReaction(userReaction)
                .createDate(post.getCreateDate())
                .updateDate(post.getUpdateDate())
                .build();

        if (isRepost) {
            var repost = PostResponse.builder()
                    .id(entity.getId())
                    .text(entity.getRepostText())
                    .userId(entity.getUser().getId().toString())
                    .username(entity.getUser().getUsername())
                    .parentId(entity.getRepostId())
                    .isRepost(true)
                    .build();
            postResponse.setRepost(repost);
        }

        if (post.getPoll() == null) return postResponse;

        List<OptionResponse> options = post.getPoll().getOptions().stream()
                .map(option -> OptionResponse.builder()
                        .id(option.getId())
                        .label(option.getLabel())
                        .voteShare(getVoteShare(option))
                        .build())
                .collect(Collectors.toList());

        PollResponse pollResponse = PollResponse.builder()
                .id(post.getPoll().getId())
                .options(options)
                .votedOption(getVotedPollOptionLabel(post.getPoll(), post.getUser().getId()))
                .voteCount(getVoteCount(post.getPoll()))
                .build();

        postResponse.setPoll(pollResponse);
        return postResponse;
    }

    private Integer getVoteCount(Poll poll) {
        return poll.getOptions().stream()
                .map(option -> option.getVotes().size())
                .reduce(0, Integer::sum);
    }

    private String getVoteShare(PollOption option) {
        if (option.getPoll() == null) return "0%";
        Integer totalVoteCount = getVoteCount(option.getPoll());
        int optionVoteNum = option.getVotes() != null ? option.getVotes().size() : 0;
        if (totalVoteCount < 1) return "0%";
        return (optionVoteNum * 100 / totalVoteCount) + "%";
    }

    private String getVotedPollOptionLabel(Poll poll, UUID userId) {
        return poll.getOptions().stream()
                .filter(option -> option.getVotes().contains(userId))
                .findFirst()
                .map(PollOption::getLabel)
                .orElse("not voted");
    }

    private User getLoggedUser() {
        return userService.getSignedUser();
    }

    public Post repostRequestToEntity(RepostRequest repost) {
        return Post.builder()
                .user(getLoggedUser())
                .repostId(repost.getPostId().toString())
                .repostText(repost.getText())
                .build();
    }

}
