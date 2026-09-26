package dev.thural.quietspace.domain.user.api;

import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound query port owned by the user domain.
 *
 * <p>Cross-domain read access (author names, member lists, actor info) goes
 * through this port — never through {@code UserRepository} or
 * {@code UserService} directly. Authentication ({@code getSignedUser}) stays
 * on {@code UserService}; this port is for display data only.</p>
 */
public interface UserQueryPort {

    /**
     * @return summary snapshot, or empty if no user with the id exists
     */
    Optional<UserSummaryDTO> getUserSummary(UUID userId);

    /**
     * Batch variant to avoid N+1 lookups when rendering lists.
     *
     * @return summaries keyed by user id; missing ids are absent from the map
     */
    Map<UUID, UserSummaryDTO> getUsersSummary(Set<UUID> userIds);

    /**
     * Authorization-support lookup: resolve a user id from either the email
     * (STOMP/HTTP principal form) or the username.
     *
     * @return user id, or empty if no user matches
     */
    Optional<UUID> findUserIdByUsernameOrEmail(String usernameOrEmail);

    /**
     * Cross-module search support (e.g. post text search matching author names).
     * Pagination-safe: callers use the ids in an SQL {@code IN} predicate.
     *
     * @return ids of users whose username/email/firstname/lastname match; empty if none
     */
    Set<UUID> searchUserIds(String keyword);
}
