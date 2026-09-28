package dev.thural.quietspace.domain.reaction;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.core.shared.util.PageUtils;
import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.shared.util.PagingProvider.DEFAULT_SORT_OPTION;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceImplTest {

    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private ReactionMapper reactionMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private ReactionServiceImpl reactionService;

    private User user;
    private UUID userId;
    private Reaction reaction;
    private ReactionRequest reactionRequest;
    private UUID contentId;

    private PageRequest pageRequest;
    private Page<Reaction> reactionPage;

    @BeforeEach
    void setUp() {
        this.userId = UUID.randomUUID();
        this.contentId = UUID.randomUUID();

        this.user = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .build();

        this.reaction = Reaction.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .contentType(EntityType.POST)
                .contentId(contentId)
                .userId(userId)
                .username(user.getUsername())
                .build();

        this.reactionRequest = ReactionRequest.builder()
                .contentId(contentId)
                .contentType(reaction.getContentType())
                .userId(userId)
                .reactionType(reaction.getReactionType())
                .build();

        this.pageRequest = buildPageRequest(0, 25, DEFAULT_SORT_OPTION);
        this.reactionPage = PageUtils.pageFromList(List.of(reaction), pageRequest);
    }

    @Test
    void handleReactionRemoveLike_shouldSucceed() {
        when(userService.getSignedUser()).thenReturn(user);
        when(reactionRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.of(reaction));

        reactionService.handleReaction(reactionRequest);

        verify(reactionRepository, times(1)).deleteById(reaction.getId());
    }

    @Test
    void handleReactionAddLike_shouldSucceed() {
        when(userService.getSignedUser()).thenReturn(user);
        when(reactionMapper.reactionRequestToEntity(any(ReactionRequest.class))).thenReturn(reaction);
        when(reactionRepository.findByContentIdAndUserId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        reactionService.handleReaction(ReactionRequest.builder()
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .build());

        verify(reactionRepository, times(1)).save(any(Reaction.class));
    }


    @Test
    void getUserReactionByContentId() {
        when(userService.getSignedUser()).thenReturn(user);
        when(reactionRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.of(reaction));
        when(reactionMapper.reactionEntityToResponse(any(Reaction.class))).thenReturn(ReactionResponse.builder().build());

        assertThat(reactionService.getUserReactionByContentId(contentId)).isNotEmpty();
    }

    @Test
    void getLikesByContentId() {
        when(reactionRepository.findAllByContentIdAndReactionType(eq(contentId), eq(ReactionType.LIKE), any(PageRequest.class))).thenReturn(reactionPage);
        when(reactionMapper.reactionEntityToResponse(any(Reaction.class))).thenReturn(ReactionResponse.builder().build());

        Page<ReactionResponse> responsePage = reactionService.getReactionsByContentIdAndReactionType(contentId, ReactionType.LIKE, 1, 25);
        assertThat(responsePage).size().isEqualTo(1);
    }

    @Test
    void getLikeCountByContentId() {
        when(reactionRepository.countByContentIdAndReactionType(contentId, ReactionType.LIKE)).thenReturn(3);

        Integer likeCount = reactionService.countByContentIdAndReactionType(contentId, ReactionType.LIKE);

        assertThat(likeCount).isEqualTo(3);
    }

    @Test
    void getReactionsByContentId() {
        when(reactionRepository.findAllByContentIdAndContentType(eq(contentId), eq(EntityType.POST), any(PageRequest.class))).thenReturn(reactionPage);
        when(reactionMapper.reactionEntityToResponse(any(Reaction.class))).thenReturn(ReactionResponse.builder().build());

        Page<ReactionResponse> responsePage = reactionService.getReactionsByContentIdAndContentType(contentId, EntityType.POST, 1, 25);
        assertThat(responsePage).isNotEmpty();
    }

    @Test
    void addReaction_shouldCreateNewReaction() {
        when(userService.getSignedUser()).thenReturn(user);
        when(reactionRepository.findByContentIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(reactionMapper.reactionRequestToEntity(any(ReactionRequest.class))).thenReturn(reaction);

        reactionService.addReaction(reactionRequest);

        verify(reactionRepository).save(any(Reaction.class));
    }

    @Test
    void addReaction_givenExisting_shouldUpdateType() {
        Reaction existing = Reaction.builder().id(UUID.randomUUID()).reactionType(ReactionType.DISLIKE).build();
        when(userService.getSignedUser()).thenReturn(user);
        when(reactionRepository.findByContentIdAndUserId(contentId, userId)).thenReturn(Optional.of(existing));

        reactionService.addReaction(reactionRequest);

        assertThat(existing.getReactionType()).isEqualTo(ReactionType.LIKE);
        verify(reactionRepository).save(existing);
    }

    @Test
    void removeReaction_shouldDeleteReaction() {
        UUID reactionId = UUID.randomUUID();

        reactionService.removeReaction(reactionId);

        verify(reactionRepository).deleteById(reactionId);
    }

    @Test
    void getReactionsByUserId() {
        when(reactionRepository.findAllByUserIdAndContentType(eq(userId), eq(EntityType.POST), any(PageRequest.class))).thenReturn(reactionPage);
        when(reactionMapper.reactionEntityToResponse(any(Reaction.class))).thenReturn(ReactionResponse.builder().build());

        Page<ReactionResponse> responsePage = reactionService.getReactionsByUserIdAndContentType(userId, EntityType.POST, 1, 25);
        assertThat(responsePage).isNotEmpty();
    }
}