package dev.thural.quietspace.domain.user.api;

import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default {@link UserQueryPort} implementation backed by {@link UserRepository}.
 */
@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserSummaryDTO> getUserSummary(UUID userId) {
        return userRepository.findById(userId).map(this::toSummary);
    }

    @Override
    public Map<UUID, UserSummaryDTO> getUsersSummary(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::toSummary));
    }

    @Override
    public Optional<UUID> findUserIdByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findUserEntityByEmail(usernameOrEmail)
                .or(() -> userRepository.findUserByUsername(usernameOrEmail))
                .map(User::getId);
    }

    @Override
    public Set<UUID> searchUserIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptySet();
        }
        return userRepository.findAllBySearchTerm(keyword, Pageable.unpaged()).stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    private UserSummaryDTO toSummary(User user) {
        String displayName = Stream.of(user.getFirstname(), user.getLastname())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
        if (!StringUtils.hasText(displayName)) {
            displayName = user.getUsername();
        }
        return new UserSummaryDTO(
                user.getId(),
                user.getUsername(),
                displayName,
                user.getPhotoId(),
                user.getStatusType()
        );
    }
}
