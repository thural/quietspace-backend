package dev.thural.quietspace.reaction;

import dev.thural.quietspace.reaction.Reaction;
import dev.thural.quietspace.reaction.ReactionMapper;
import dev.thural.quietspace.reaction.dto.ReactionRequest;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReactionMapper reactionMapper;

    private ReactionRequest reactionRequest;
    private Reaction reaction;
    private User user;
    private UUID reactionId;
    private UUID userId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        reactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@test.com")
                .build();

        reactionRequest = ReactionRequest.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        reaction = Reaction.builder()
                .id(reactionId)
                .userId(userId)
                .username("testuser")
                .contentId(contentId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    @Test
    void reactionRequestToEntity_shouldConvertRequestToEntity() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(reactionRequest.getUserId());
        assertThat(result.getContentId()).isEqualTo(reactionRequest.getContentId());
        assertThat(result.getContentType()).isEqualTo(reactionRequest.getContentType());
        assertThat(result.getReactionType()).isEqualTo(reactionRequest.getReactionType());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionRequestToEntity_shouldHandleNullUsernameWhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(reactionRequest.getUserId());
        assertThat(result.getContentId()).isEqualTo(reactionRequest.getContentId());
        assertThat(result.getContentType()).isEqualTo(reactionRequest.getContentType());
        assertThat(result.getReactionType()).isEqualTo(reactionRequest.getReactionType());
        assertThat(result.getUsername()).isNull();

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionRequestToEntity_shouldHandleDifferentReactionTypes() {
        // Test all reaction types
        ReactionType[] reactionTypes = ReactionType.values();
        
        for (ReactionType type : reactionTypes) {
            // Given
            reactionRequest.setReactionType(type);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReactionType()).isEqualTo(type);

            // Reset for next iteration
            reset(userRepository);
        }
    }

    @Test
    void reactionRequestToEntity_shouldHandleDifferentEntityTypes() {
        // Test all entity types
        EntityType[] entityTypes = EntityType.values();
        
        for (EntityType type : entityTypes) {
            // Given
            reactionRequest.setContentType(type);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContentType()).isEqualTo(type);

            // Reset for next iteration
            reset(userRepository);
        }
    }

    @Test
    void reactionEntityToResponse_shouldConvertEntityToResponse() {
        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(reaction.getId());
        assertThat(result.getUserId()).isEqualTo(reaction.getUserId());
        assertThat(result.getContentId()).isEqualTo(reaction.getContentId());
        assertThat(result.getUsername()).isEqualTo(reaction.getUsername());
        assertThat(result.getReactionType()).isEqualTo(reaction.getReactionType());
        assertThat(result.getCreateDate()).isEqualTo(reaction.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(reaction.getUpdateDate());
    }

    @Test
    void reactionEntityToResponse_shouldHandleNullFields() {
        // Given
        reaction.setUsername(null);
        reaction.setReactionType(null);

        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isNull();
        assertThat(result.getReactionType()).isNull();
    }

    @Test
    void reactionEntityToResponse_shouldHandleLikeReaction() {
        // Given
        reaction.setReactionType(ReactionType.LIKE);

        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReactionType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    void reactionEntityToResponse_shouldHandleDislikeReaction() {
        // Given
        reaction.setReactionType(ReactionType.DISLIKE);

        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReactionType()).isEqualTo(ReactionType.DISLIKE);
    }

    @Test
    void getUserNameById_shouldReturnUsernameWhenUserFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        String result = reactionMapper.getUserNameById(userId);

        // Then
        assertThat(result).isEqualTo(user.getUsername());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserNameById_shouldReturnNullWhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        String result = reactionMapper.getUserNameById(userId);

        // Then
        assertThat(result).isNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void reactionRequestToEntity_shouldCopyAllRequestFields() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(reactionRequest.getUserId());
        assertThat(result.getContentId()).isEqualTo(reactionRequest.getContentId());
        assertThat(result.getContentType()).isEqualTo(reactionRequest.getContentType());
        assertThat(result.getReactionType()).isEqualTo(reactionRequest.getReactionType());
        // BeanUtils.copyProperties should copy all matching fields
        // Username is set separately

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionEntityToResponse_shouldCopyAllEntityFields() {
        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(reaction.getId());
        assertThat(result.getUserId()).isEqualTo(reaction.getUserId());
        assertThat(result.getContentId()).isEqualTo(reaction.getContentId());
        assertThat(result.getUsername()).isEqualTo(reaction.getUsername());
        assertThat(result.getReactionType()).isEqualTo(reaction.getReactionType());
        assertThat(result.getCreateDate()).isEqualTo(reaction.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(reaction.getUpdateDate());
        // BeanUtils.copyProperties should copy all matching fields
    }

    @Test
    void reactionRequestToEntity_shouldHandlePostReaction() {
        // Given
        reactionRequest.setContentType(EntityType.POST);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo(EntityType.POST);

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionRequestToEntity_shouldHandleCommentReaction() {
        // Given
        reactionRequest.setContentType(EntityType.COMMENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo(EntityType.COMMENT);

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionRequestToEntity_shouldHandleMessageReaction() {
        // Given
        reactionRequest.setContentType(EntityType.MESSAGE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        Reaction result = reactionMapper.reactionRequestToEntity(reactionRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo(EntityType.MESSAGE);

        verify(userRepository).findById(userId);
    }

    @Test
    void reactionEntityToResponse_shouldHandleAllReactionTypes() {
        // Test all reaction types in response
        ReactionType[] reactionTypes = ReactionType.values();
        
        for (ReactionType type : reactionTypes) {
            // Given
            reaction.setReactionType(type);

            // When
            ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReactionType()).isEqualTo(type);
        }
    }

    @Test
    void reactionEntityToResponse_shouldHandleNullUsername() {
        // Given
        reaction.setUsername(null);

        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isNull();
    }

    @Test
    void reactionEntityToResponse_shouldHandleEmptyUsername() {
        // Given
        reaction.setUsername("");

        // When
        ReactionResponse result = reactionMapper.reactionEntityToResponse(reaction);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEmpty();
    }
}
