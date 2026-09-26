package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.post.dto.PostResponse;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostSecurityServiceTest {

    @Mock
    private PostService postService;
    @Mock
    private UserQueryPort userQueryPort;

    @InjectMocks
    private PostSecurityService postSecurityService;

    private final UUID postId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @Test
    void canAccess_whenUserIsOwner_shouldReturnTrue() {
        when(userQueryPort.findUserIdByUsernameOrEmail("owner@test.com"))
                .thenReturn(Optional.of(ownerId));
        when(postService.getPostById(postId))
                .thenReturn(Optional.of(PostResponse.builder().userId(ownerId.toString()).build()));

        boolean result = postSecurityService.canAccess(postId, "owner@test.com");

        assertThat(result).isTrue();
    }

    @Test
    void canAccess_whenUserIsNotOwner_shouldReturnFalse() {
        when(userQueryPort.findUserIdByUsernameOrEmail("other@test.com"))
                .thenReturn(Optional.of(otherUserId));
        when(postService.getPostById(postId))
                .thenReturn(Optional.of(PostResponse.builder().userId(ownerId.toString()).build()));

        boolean result = postSecurityService.canAccess(postId, "other@test.com");

        assertThat(result).isFalse();
    }

    @Test
    void canAccess_whenPostDoesNotExist_shouldReturnFalse() {
        when(userQueryPort.findUserIdByUsernameOrEmail("owner@test.com"))
                .thenReturn(Optional.of(ownerId));
        when(postService.getPostById(postId)).thenReturn(Optional.empty());

        boolean result = postSecurityService.canAccess(postId, "owner@test.com");

        assertThat(result).isFalse();
    }

    @Test
    void canAccess_whenUserDoesNotExist_shouldReturnFalse() {
        when(userQueryPort.findUserIdByUsernameOrEmail("unknown@test.com")).thenReturn(Optional.empty());

        boolean result = postSecurityService.canAccess(postId, "unknown@test.com");

        assertThat(result).isFalse();
    }
}
