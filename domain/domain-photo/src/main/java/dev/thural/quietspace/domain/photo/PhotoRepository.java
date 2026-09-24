package dev.thural.quietspace.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    Optional<Photo> findByName(String name);

    void deleteByEntityId(UUID entityId);

    Optional<Photo> findByEntityId(UUID entityId);
}
