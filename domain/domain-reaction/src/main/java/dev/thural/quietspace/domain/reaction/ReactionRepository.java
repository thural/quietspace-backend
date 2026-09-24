package dev.thural.quietspace.reaction;

import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    Page<Reaction> findAllByContentId(UUID contentId, Pageable pageable);

    Page<Reaction> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

    Optional<Reaction> findByContentIdAndUserId(UUID commentId, UUID id);

    Page<Reaction> findAllByContentTypeAndUserId(EntityType contentType, UUID userId, Pageable pageable);

    Page<Reaction> findAllByContentIdAndContentType(UUID contentId, EntityType contentType, Pageable pageable);

    Page<Reaction> findAllByUserIdAndContentType(UUID userId, EntityType contentType, Pageable pageable);

    Integer countByContentIdAndReactionType(UUID contentId, ReactionType reactionType);

    Page<Reaction> findAllByContentIdAndReactionType(UUID contentId, ReactionType reactionType, Pageable pageable);
}