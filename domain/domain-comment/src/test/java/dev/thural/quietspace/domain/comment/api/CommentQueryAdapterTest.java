package dev.thural.quietspace.domain.comment.api;

import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentRepository;
import dev.thural.quietspace.domain.comment.api.dto.CommentSummaryDTO;
import dev.thural.quietspace.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentQueryAdapterTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentQueryAdapter adapter;

    private static Comment comment(UUID id, UUID postId, User author) {
        return Comment.builder()
                .id(id)
                .postId(postId)
                .userId(author.getId())
                .text("sample text")
                .parentId(UUID.randomUUID())
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
    void getCommentSummary_givenExistingComment_shouldReturnSnapshot() {
        UUID id = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment(id, postId, author(authorId))));

        Optional<CommentSummaryDTO> result = adapter.getCommentSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().postId()).isEqualTo(postId);
        assertThat(result.get().authorId()).isEqualTo(authorId);
        assertThat(result.get().text()).isEqualTo("sample text");
    }

    @Test
    void getCommentSummary_givenMissingComment_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.getCommentSummary(id)).isEmpty();
    }

    @Test
    void getCommentsSummary_givenIds_shouldReturnKeyedMap() {
        UUID id1 = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User author = author(UUID.randomUUID());
        when(commentRepository.findAllById(Set.of(id1)))
                .thenReturn(List.of(comment(id1, postId, author)));

        Map<UUID, CommentSummaryDTO> result = adapter.getCommentsSummary(Set.of(id1));

        assertThat(result).hasSize(1);
        assertThat(result.get(id1).postId()).isEqualTo(postId);
    }

    @Test
    void getCommentsSummary_givenEmptySet_shouldReturnEmptyMapWithoutQuery() {
        assertThat(adapter.getCommentsSummary(Set.of())).isEmpty();
    }

    @Test
    void countCommentsByPostId_shouldReturnTotalElements() {
        UUID postId = UUID.randomUUID();
        User author = author(UUID.randomUUID());
        when(commentRepository.findAllByPostId(postId, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(
                        comment(UUID.randomUUID(), postId, author),
                        comment(UUID.randomUUID(), postId, author))));

        assertThat(adapter.countCommentsByPostId(postId)).isEqualTo(2);
    }

    @Test
    void findPostIdsByUserId_shouldReturnDistinctPostIds() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(commentRepository.findDistinctPostIdsByUserId(userId))
                .thenReturn(Set.of(postId));

        assertThat(adapter.findPostIdsByUserId(userId)).containsExactly(postId);
    }

    @Test
    void findPostIdsByUserId_givenNull_shouldReturnEmptyWithoutQuery() {
        assertThat(adapter.findPostIdsByUserId(null)).isEmpty();
    }
}
