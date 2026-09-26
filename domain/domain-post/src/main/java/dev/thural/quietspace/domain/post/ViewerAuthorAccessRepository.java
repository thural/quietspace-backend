package dev.thural.quietspace.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ViewerAuthorAccessRepository extends JpaRepository<ViewerAuthorAccess, ViewerAuthorAccess.ViewerAuthorAccessId> {

    boolean existsByViewerIdAndAuthorId(UUID viewerId, UUID authorId);

    void deleteByViewerIdAndAuthorId(UUID viewerId, UUID authorId);
}
