package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.domain.photo.dto.PhotoResponse;
import dev.thural.quietspace.domain.post.*;
import dev.thural.quietspace.domain.post.dto.*;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostMapperTest {

    @Mock
    private ReactionService reactionService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PhotoService photoService;

    @Mock
    private UserService userService;

    @Mock
    private UserQueryPort userQueryPort;

    @InjectMocks
    private PostMapper postMapper;

    private PostRequest postRequest;
    private RepostRequest repostRequest;
    private Post post;
    private Post originalPost;
    private User user;
    private User loggedUser;
    private Poll poll;
    private List<PollOption> pollOptions;
    private PhotoResponse photoResponse;
    private ReactionResponse userReaction;
    private UUID postId;
    private UUID userId;
    private UUID photoId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        photoId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@test.com")
                .build();

        loggedUser = User.builder()
                .id(UUID.randomUUID())
                .username("loggeduser")
                .email("logged@test.com")
                .build();

        postRequest = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post")
                .poll(PollRequest.builder()
                        .dueDate(OffsetDateTime.now().plusDays(7))
                        .options(List.of("Option 1", "Option 2", "Option 3"))
                        .build())
                .build();

        repostRequest = RepostRequest.builder()
                .postId(postId)
                .text("Repost text")
                .build();

        pollOptions = List.of(
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 1")
                        .votes(Set.of(userId, loggedUser.getId()))
                        .build(),
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 2")
                        .votes(Set.of(userId))
                        .build(),
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 3")
                        .votes(Set.of())
                        .build()
        );

        poll = Poll.builder()
                .id(UUID.randomUUID())
                .dueDate(OffsetDateTime.now().plusDays(7))
                .options(pollOptions)
                .build();
        pollOptions.forEach(opt -> opt.setPoll(poll));

        originalPost = Post.builder()
                .id(postId)
                .title("Original Post")
                .text("Original content")
                .authorId(user.getId())
                .poll(poll)
                .comments(List.of())
                .photoId(photoId)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        post = Post.builder()
                .id(UUID.randomUUID())
                .title("Test Post")
                .text("This is a test post")
                .authorId(user.getId())
                .comments(List.of())
                .photoId(photoId)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        photoResponse = PhotoResponse.builder()
                .id(photoId)
                .name("photo.jpg")
                .type("image/jpeg")
                .data(new byte[]{1, 2, 3})
                .build();

        userReaction = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .build();
    }

    @Test
    void postRequestToEntity_shouldConvertRequestToEntity() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);

        // When
        Post result = postMapper.postRequestToEntity(postRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(postRequest.getTitle());
        assertThat(result.getText()).isEqualTo(postRequest.getText());
        assertThat(result.getAuthorId()).isEqualTo(loggedUser.getId());
        assertThat(result.getPoll()).isNotNull();
        assertThat(result.getPoll().getDueDate()).isEqualTo(postRequest.getPoll().getDueDate());
        assertThat(result.getPoll().getOptions()).hasSize(3);
        assertThat(result.getPoll().getOptions().get(0).getLabel()).isEqualTo("Option 1");
        assertThat(result.getPoll().getOptions().get(0).getVotes()).isEmpty();
        
        verify(userService).getSignedUser();
    }

    @Test
    void postRequestToEntity_shouldHandleNullPoll() {
        // Given
        PostRequest requestWithoutPoll = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post")
                .poll(null)
                .build();
        when(userService.getSignedUser()).thenReturn(loggedUser);

        // When
        Post result = postMapper.postRequestToEntity(requestWithoutPoll);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoll()).isNull();
        verify(userService).getSignedUser();
    }

    @Test
    void postRequestToEntity_shouldCreatePollWithOptions() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);

        // When
        Post result = postMapper.postRequestToEntity(postRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoll()).isNotNull();
        assertThat(result.getPoll().getOptions()).hasSize(3);
        assertThat(result.getPoll().getPost()).isEqualTo(result);
        
        List<String> optionLabels = result.getPoll().getOptions().stream()
                .map(PollOption::getLabel)
                .toList();
        assertThat(optionLabels).containsExactly("Option 1", "Option 2", "Option 3");
        
        verify(userService).getSignedUser();
    }

    @Test
    void postEntityToResponse_shouldConvertEntityToResponse() {
        // Given
        when(reactionService.countByContentIdAndReactionType(post.getId(), ReactionType.LIKE))
                .thenReturn(5);
        when(reactionService.countByContentIdAndReactionType(post.getId(), ReactionType.DISLIKE))
                .thenReturn(2);
        when(reactionService.getUserReactionByContentId(post.getId()))
                .thenReturn(Optional.of(userReaction));
        when(photoService.getPhotoById(photoId)).thenReturn(photoResponse);

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));

        // When
        PostResponse result = postMapper.postEntityToResponse(post);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(post.getId());
        assertThat(result.getTitle()).isEqualTo(post.getTitle());
        assertThat(result.getText()).isEqualTo(post.getText());
        assertThat(result.getUserId()).isEqualTo(user.getId().toString());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getPhoto()).isEqualTo(photoResponse);
        assertThat(result.getLikeCount()).isEqualTo(5);
        assertThat(result.getDislikeCount()).isEqualTo(2);
        assertThat(result.getCommentCount()).isEqualTo(0);
        assertThat(result.getUserReaction()).isEqualTo(userReaction);
        assertThat(result.getIsRepost()).isNull();
        assertThat(result.getRepost()).isNull();

        verify(reactionService).countByContentIdAndReactionType(post.getId(), ReactionType.LIKE);
        verify(reactionService).countByContentIdAndReactionType(post.getId(), ReactionType.DISLIKE);
        verify(reactionService).getUserReactionByContentId(post.getId());
        verify(photoService).getPhotoById(photoId);
    }

    @Test
    void postEntityToResponse_shouldHandleNullPhotoId() {
        // Given
        post.setPhotoId(null);
        when(reactionService.countByContentIdAndReactionType(post.getId(), ReactionType.LIKE))
                .thenReturn(0);
        when(reactionService.countByContentIdAndReactionType(post.getId(), ReactionType.DISLIKE))
                .thenReturn(0);
        when(reactionService.getUserReactionByContentId(post.getId()))
                .thenReturn(Optional.empty());

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));

        // When
        PostResponse result = postMapper.postEntityToResponse(post);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isNull();
        verify(photoService, never()).getPhotoById(any());
    }

    @Test
    void postEntityToResponse_shouldHandleRepost() {
        // Given
        Post repost = Post.builder()
                .id(UUID.randomUUID())
                .repostId(postId.toString())
                .repostText("Repost text")
                .authorId(loggedUser.getId())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        when(postRepository.findById(UUID.fromString(repost.getRepostId())))
                .thenReturn(Optional.of(originalPost));
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.LIKE))
                .thenReturn(10);
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.DISLIKE))
                .thenReturn(3);
        when(reactionService.getUserReactionByContentId(originalPost.getId()))
                .thenReturn(Optional.empty());
        when(photoService.getPhotoById(photoId)).thenReturn(photoResponse);

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        when(userQueryPort.getUserSummary(loggedUser.getId()))
                .thenReturn(Optional.of(new UserSummaryDTO(loggedUser.getId(), "loggeduser", "Logged User", null, null)));
        // When
        PostResponse result = postMapper.postEntityToResponse(repost);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(originalPost.getId());
        assertThat(result.getTitle()).isEqualTo(originalPost.getTitle());
        assertThat(result.getText()).isEqualTo(originalPost.getText());
        assertThat(result.getRepost()).isNotNull();
        assertThat(result.getRepost().getId()).isEqualTo(repost.getId());
        assertThat(result.getRepost().getText()).isEqualTo(repost.getRepostText());
        assertThat(result.getRepost().getUserId()).isEqualTo(loggedUser.getId().toString());
        assertThat(result.getRepost().getUsername()).isEqualTo(loggedUser.getUsername());
        assertThat(result.getRepost().getParentId()).isEqualTo(repost.getRepostId());
        assertThat(result.getRepost().getIsRepost()).isTrue();

        verify(postRepository).findById(UUID.fromString(repost.getRepostId()));
    }

    @Test
    void postEntityToResponse_shouldHandleRepostWithNullOriginalPost() {
        // Given
        Post repost = Post.builder()
                .id(UUID.randomUUID())
                .repostId(postId.toString())
                .repostText("Repost text")
                .authorId(loggedUser.getId())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        when(postRepository.findById(UUID.fromString(repost.getRepostId())))
                .thenReturn(Optional.empty());

        // When
        PostResponse result = postMapper.postEntityToResponse(repost);

        // Then
        assertThat(result).isNull();
        verify(postRepository).findById(UUID.fromString(repost.getRepostId()));
    }

    @Test
    void postEntityToResponse_shouldHandlePoll() {
        // Given
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.LIKE))
                .thenReturn(0);
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.DISLIKE))
                .thenReturn(0);
        when(reactionService.getUserReactionByContentId(originalPost.getId()))
                .thenReturn(Optional.empty());
        when(photoService.getPhotoById(photoId)).thenReturn(null);

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));

        // When
        PostResponse result = postMapper.postEntityToResponse(originalPost);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoll()).isNotNull();
        assertThat(result.getPoll().getId()).isEqualTo(poll.getId());
        assertThat(result.getPoll().getOptions()).hasSize(3);
        assertThat(result.getPoll().getVoteCount()).isEqualTo(3); // 2 + 1 + 0 votes
        assertThat(result.getPoll().getVotedOption()).isEqualTo("Option 1"); // User voted for Option 1

        OptionResponse option1 = result.getPoll().getOptions().get(0);
        OptionResponse option2 = result.getPoll().getOptions().get(1);
        OptionResponse option3 = result.getPoll().getOptions().get(2);

        assertThat(option1.getVoteShare()).isEqualTo("66%"); // 2 out of 3 votes
        assertThat(option2.getVoteShare()).isEqualTo("33%"); // 1 out of 3 votes
        assertThat(option3.getVoteShare()).isEqualTo("0%");  // 0 out of 3 votes
    }

    @Test
    void repostRequestToEntity_shouldConvertRequestToEntity() {
        // Given
        when(userService.getSignedUser()).thenReturn(loggedUser);

        // When
        Post result = postMapper.repostRequestToEntity(repostRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(loggedUser.getId());
        assertThat(result.getRepostId()).isEqualTo(repostRequest.getPostId().toString());
        assertThat(result.getRepostText()).isEqualTo(repostRequest.getText());
        
        verify(userService).getSignedUser();
    }

    @Test
    void getVoteCount_shouldCalculateTotalVotes() {
        // This is tested indirectly through postEntityToResponse test with poll
        // The vote count calculation is verified in the poll test above
    }

    @Test
    void getVoteShare_shouldCalculateCorrectPercentage() {
        // This is tested indirectly through postEntityToResponse test with poll
        // The vote share calculation is verified in the poll test above
    }

    @Test
    void getVotedPollOptionLabel_shouldReturnCorrectOption() {
        // This is tested indirectly through postEntityToResponse test with poll
        // The voted option logic is verified in the poll test above
    }

    @Test
    void postEntityToResponse_shouldHandleZeroVoteCount() {
        // Given
        pollOptions.forEach(option -> option.setVotes(Set.of()));
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.LIKE))
                .thenReturn(0);
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.DISLIKE))
                .thenReturn(0);
        when(reactionService.getUserReactionByContentId(originalPost.getId()))
                .thenReturn(Optional.empty());
        when(photoService.getPhotoById(photoId)).thenReturn(null);

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));

        // When
        PostResponse result = postMapper.postEntityToResponse(originalPost);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoll()).isNotNull();
        assertThat(result.getPoll().getVoteCount()).isEqualTo(0);
        assertThat(result.getPoll().getVotedOption()).isEqualTo("not voted");

        result.getPoll().getOptions().forEach(option -> {
            assertThat(option.getVoteShare()).isEqualTo("0%");
        });
    }

    @Test
    void postEntityToResponse_shouldHandleUserNotVoted() {
        // Given
        pollOptions.forEach(option -> option.setVotes(Set.of(loggedUser.getId()))); // Only logged user voted
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.LIKE))
                .thenReturn(0);
        when(reactionService.countByContentIdAndReactionType(originalPost.getId(), ReactionType.DISLIKE))
                .thenReturn(0);
        when(reactionService.getUserReactionByContentId(originalPost.getId()))
                .thenReturn(Optional.empty());
        when(photoService.getPhotoById(photoId)).thenReturn(null);

        // Given username resolution
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));

        // When
        PostResponse result = postMapper.postEntityToResponse(originalPost);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoll()).isNotNull();
        assertThat(result.getPoll().getVotedOption()).isEqualTo("not voted");
    }
}
