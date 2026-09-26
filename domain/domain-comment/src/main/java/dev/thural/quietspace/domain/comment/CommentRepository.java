package dev.thural.quietspace.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findAllByPostId(UUID postId, Pageable pageable);

    /**
     * Synchronous cascade support for post deletion (called within the post
     * service transaction — see {@code PostServiceImpl.deletePost}).
     */
    void deleteAllByPostId(UUID postId);

    Integer countByParentIdAndPostId(UUID parentId, UUID postId);

    @Transactional
    void deleteAllByParentId(UUID parentId);

    Page<Comment> findAllByParentId(UUID commentId, Pageable pageable);

    Page<Comment> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT DISTINCT c.postId FROM Comment c WHERE c.userId = :userId")
    java.util.Set<UUID> findDistinctPostIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.postId = :postId " +
            "AND c.userId = :userId " +
            "ORDER BY c.updateDate DESC, c.createDate DESC " +
            "LIMIT 1")
    Optional<Comment> findLatestCommentByPostAndUserByUpdateDate(@Param("postId") UUID postId, @Param("userId") UUID userId);
}
