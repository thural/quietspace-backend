package dev.thural.quietspace.domain.post.api;

import dev.thural.quietspace.domain.post.Post;
import dev.thural.quietspace.domain.post.PostRepository;
import dev.thural.quietspace.domain.post.api.dto.PostSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default {@link PostQueryPort} implementation backed by {@link PostRepository}.
 */
@Component
@RequiredArgsConstructor
public class PostQueryAdapter implements PostQueryPort {

    private final PostRepository postRepository;

    @Override
    public Optional<PostSummaryDTO> getPostSummary(UUID postId) {
        return postRepository.findById(postId).map(this::toSummary);
    }

    @Override
    public Map<UUID, PostSummaryDTO> getPostsSummary(Set<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, this::toSummary));
    }

    private PostSummaryDTO toSummary(Post post) {
        return new PostSummaryDTO(
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getText(),
                post.getPhotoId()
        );
    }
}
