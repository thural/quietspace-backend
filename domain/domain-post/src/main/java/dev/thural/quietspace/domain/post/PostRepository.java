package dev.thural.quietspace.post;

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

    @Query("SELECT p FROM Post p JOIN p.savedByUsers u WHERE u.id = :userId")
    Page<Post> findSavedPostsByUserId(UUID userId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p JOIN p.comments c WHERE c.user.id = :userId")
    Page<Post> findByCommentsUserId(UUID userId, Pageable pageable);

    void deleteByRepostId(String repostId);
}