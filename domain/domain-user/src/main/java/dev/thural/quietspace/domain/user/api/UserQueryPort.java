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
}
