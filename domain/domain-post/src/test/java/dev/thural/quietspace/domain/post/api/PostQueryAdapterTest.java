package dev.thural.quietspace.domain.post.api;

import dev.thural.quietspace.domain.post.Post;
import dev.thural.quietspace.domain.post.PostRepository;
import dev.thural.quietspace.domain.post.api.dto.PostSummaryDTO;
import dev.thural.quietspace.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostQueryAdapterTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostQueryAdapter adapter;

    private static Post post(UUID id, User author) {
        return Post.builder()
                .id(id)
                .authorId(author.getId())
                .title("title")
                .text("sample text")
                .build();
    }

    private static User author(UUID id) {
        return User.builder()
                .id(id)
                .username("author")
                .email("author@test.com")
                .password("secret")
                .build();
    }

    @Test
    void getPostSummary_givenExistingPost_shouldReturnSnapshot() {
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.of(post(id, author(authorId))));

        Optional<PostSummaryDTO> result = adapter.getPostSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().authorId()).isEqualTo(authorId);
        assertThat(result.get().text()).isEqualTo("sample text");
    }

    @Test
    void getPostSummary_givenMissingPost_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.getPostSummary(id)).isEmpty();
    }

    @Test
    void getPostsSummary_givenIds_shouldReturnKeyedMap() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User author = author(UUID.randomUUID());
        when(postRepository.findAllById(Set.of(id1, id2)))
                .thenReturn(List.of(post(id1, author), post(id2, author)));

        Map<UUID, PostSummaryDTO> result = adapter.getPostsSummary(Set.of(id1, id2));

        assertThat(result).hasSize(2);
        assertThat(result.get(id1).authorId()).isEqualTo(author.getId());
    }

    @Test
    void getPostsSummary_givenEmptySet_shouldReturnEmptyMapWithoutQuery() {
        assertThat(adapter.getPostsSummary(Set.of())).isEmpty();
    }
}
