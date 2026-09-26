package dev.thural.quietspace.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostAuthorVisibilityRepository extends JpaRepository<PostAuthorVisibility, UUID> {
}
