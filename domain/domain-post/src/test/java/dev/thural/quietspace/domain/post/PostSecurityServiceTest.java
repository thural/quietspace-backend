package dev.thural.quietspace.post;

import dev.thural.quietspace.post.dto.PostResponse;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
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
    private UserRepository userRepository;

    @InjectMocks
    private PostSecurityService postSecurityService;

    private final UUID postId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @Test
    void canAccess_whenUserIsOwner_shouldReturnTrue() {
        when(userRepository.findUserEntityByEmail("owner@test.com"))
                .thenReturn(Optional.of(User.builder().id(ownerId).build()));
        when(postService.getPostById(postId))
                .thenReturn(Optional.of(PostResponse.builder().userId(ownerId.toString()).build()));

        boolean result = postSecurityService.canAccess(postId, "owner@test.com");

        assertThat(result).isTrue();
    }

    @Test
    void canAccess_whenUserIsNotOwner_shouldReturnFalse() {
        when(userRepository.findUserEntityByEmail("other@test.com"))
                .thenReturn(Optional.of(User.builder().id(otherUserId).build()));
        when(postService.getPostById(postId))
                .thenReturn(Optional.of(PostResponse.builder().userId(ownerId.toString()).build()));

        boolean result = postSecurityService.canAccess(postId, "other@test.com");

        assertThat(result).isFalse();
    }

    @Test
    void canAccess_whenPostDoesNotExist_shouldReturnFalse() {
        when(userRepository.findUserEntityByEmail("owner@test.com"))
                .thenReturn(Optional.of(User.builder().id(ownerId).build()));
        when(postService.getPostById(postId)).thenReturn(Optional.empty());

        boolean result = postSecurityService.canAccess(postId, "owner@test.com");

        assertThat(result).isFalse();
    }

    @Test
    void canAccess_whenUserDoesNotExist_shouldReturnFalse() {
        when(userRepository.findUserEntityByEmail("unknown@test.com")).thenReturn(Optional.empty());

        boolean result = postSecurityService.canAccess(postId, "unknown@test.com");

        assertThat(result).isFalse();
    }
}
