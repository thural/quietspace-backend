package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.user.api.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("postSecurity")
@RequiredArgsConstructor
public class PostSecurityService {

    private final PostService postService;
    private final UserQueryPort userQueryPort;

    public boolean canAccess(UUID postId, String username) {
        return userQueryPort.findUserIdByUsernameOrEmail(username)
                .map(userId -> postService.getPostById(postId)
                        .map(post -> post.getUserId().equals(userId.toString()))
                        .orElse(false))
                .orElse(false);
    }
}
