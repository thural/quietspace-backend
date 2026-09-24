package dev.thural.quietspace.core.shared.ports;

import java.util.UUID;

/**
 * Shared-kernel port for user-profile mutations needed by modules that must
 * not depend on the user domain (e.g. photo handling during uploads).
 *
 * <p>Owned by core-shared and implemented by the user domain. Keeps the module
 * dependency pointing a single way: user &rarr; photo.</p>
 */
public interface UserProfilePort {

    /**
     * Resolve the id of the currently authenticated (signed-in) user.
     *
     * @return id of the signed-in user
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if no user is signed in
     */
    UUID currentUserId();

    /**
     * Record an uploaded photo as the user's profile photo.
     *
     * @param userId  the owning user id
     * @param photoId the uploaded photo id
     */
    void setProfilePhoto(UUID userId, UUID photoId);
}
