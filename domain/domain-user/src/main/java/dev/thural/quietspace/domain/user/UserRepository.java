package dev.thural.quietspace.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Page<User> findAllByUsernameIsLikeIgnoreCase(String userName, Pageable pageable);

    Optional<User> findUserEntityByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:searchTerm% OR u.email LIKE %:searchTerm%")
    Page<User> findAllBySearchTerm(String searchTerm, Pageable pageable);

    Optional<User> findUserByUsername(String username);

    Optional<User> findFirstByOrderByUsernameDesc();

    boolean existsByUsernameIgnoreCase(String admin);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
