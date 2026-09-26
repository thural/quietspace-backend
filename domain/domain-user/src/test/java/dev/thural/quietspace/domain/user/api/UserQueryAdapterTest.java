package dev.thural.quietspace.domain.user.api;

import dev.thural.quietspace.core.shared.enums.StatusType;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryAdapter adapter;

    private static User user(UUID id, String username, String firstname, String lastname) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .password("secret")
                .firstname(firstname)
                .lastname(lastname)
                .statusType(StatusType.ONLINE)
                .build();
    }

    @Test
    void getUserSummary_givenExistingUser_shouldReturnSnapshot() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(user(id, "jdoe", "John", "Doe")));

        Optional<UserSummaryDTO> result = adapter.getUserSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().username()).isEqualTo("jdoe");
        assertThat(result.get().displayName()).isEqualTo("John Doe");
        assertThat(result.get().statusType()).isEqualTo(StatusType.ONLINE);
    }

    @Test
    void getUserSummary_givenMissingUser_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.getUserSummary(id)).isEmpty();
    }

    @Test
    void getUserSummary_givenBlankNames_shouldFallBackToUsername() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(user(id, "jdoe", null, " ")));

        Optional<UserSummaryDTO> result = adapter.getUserSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("jdoe");
    }

    @Test
    void getUsersSummary_givenIds_shouldReturnKeyedMap() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User u1 = user(id1, "u1", "A", "One");
        User u2 = user(id2, "u2", "B", "Two");
        when(userRepository.findAllById(Set.of(id1, id2))).thenReturn(List.of(u1, u2));

        Map<UUID, UserSummaryDTO> result = adapter.getUsersSummary(Set.of(id1, id2));

        assertThat(result).hasSize(2);
        assertThat(result.get(id1).displayName()).isEqualTo("A One");
        assertThat(result.get(id2).username()).isEqualTo("u2");
    }

    @Test
    void getUsersSummary_givenEmptySet_shouldReturnEmptyMapWithoutQuery() {
        Map<UUID, UserSummaryDTO> result = adapter.getUsersSummary(Set.of());

        assertThat(result).isEmpty();
    }
}
