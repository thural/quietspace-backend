package dev.thural.quietspace.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    Page<Post> findAllByUserId(UUID userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.title LIKE %:query% OR p.text LIKE %:query%")
    Page<Post> findAllByQuery(String query, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.id IN :postIds")
    Page<Post> findSavedPostsByIds(java.util.List<UUID> postIds, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p WHERE p.id IN (SELECT DISTINCT c.postId FROM Comment c WHERE c.userId = :userId)")
    Page<Post> findByCommentsUserId(UUID userId, Pageable pageable);

    void deleteByRepostId(String repostId);
}