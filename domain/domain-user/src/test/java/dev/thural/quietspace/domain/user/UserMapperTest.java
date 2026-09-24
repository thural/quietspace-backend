package dev.thural.quietspace.user;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserMapper;
import dev.thural.quietspace.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private PhotoService photoService;

    @InjectMocks
    private UserMapper userMapper;

    private User user;
    private User followerUser;
    private PhotoResponse profilePhoto;
    private UUID photoId;

    @BeforeEach
    void setUp() {
        photoId = UUID.randomUUID();
        
        followerUser = User.builder()
                .id(UUID.randomUUID())
                .username("follower")
                .email("follower@test.com")
                .role(Role.USER)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@test.com")
                .password("encodedPassword")
                .photoId(photoId)
                .role(Role.USER)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        user.setFollowers(new ArrayList<>(List.of(followerUser, user)));
        user.setFollowings(new ArrayList<>(List.of(followerUser, user)));

        profilePhoto = PhotoResponse.builder()
                .id(photoId)
                .name("profile.jpg")
                .type("image/jpeg")
                .data(new byte[]{1, 2, 3})
                .build();
    }

    @Test
    void toResponse_shouldConvertUserToUserResponse() {
        // Given
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getRole()).isEqualTo(user.getRole().name());
        assertThat(result.getPhoto()).isEqualTo(profilePhoto);
        assertThat(result.getIsFollower()).isTrue(); // User is in their own followers list
        assertThat(result.getIsFollowing()).isTrue(); // User is in their own following list
        assertThat(result.getIsPrivateAccount()).isEqualTo(user.getProfileSettings().getIsPrivateAccount());
        assertThat(result.getBio()).isEqualTo(user.getProfileSettings().getBio());
        
        verify(photoService).getPhotoById(photoId);
    }

    @Test
    void toResponse_shouldHandleNullPhotoId() {
        // Given
        user.setPhotoId(null);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isNull();
        verify(photoService, never()).getPhotoById(any());
    }

    @Test
    void toResponse_shouldHandleUserNotInFollowers() {
        // Given
        user.setFollowers(List.of());
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsFollower()).isFalse();
        assertThat(result.getIsFollowing()).isTrue(); // user remains in its own following list
    }

    @Test
    void toResponse_shouldHandleUserNotInFollowing() {
        // Given
        user.setFollowings(List.of());
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsFollower()).isTrue(); // Still true because user is in followers list
        assertThat(result.getIsFollowing()).isFalse();
    }

    @Test
    void toResponse_shouldHandleNullFollowersAndFollowing() {
        // Given
        user.setFollowers(null);
        user.setFollowings(null);
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsFollower()).isFalse();
        assertThat(result.getIsFollowing()).isFalse();
    }

    @Test
    void toProfileResponse_shouldConvertUserToProfileResponse() {
        // Given
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toProfileResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getRole()).isEqualTo(user.getRole().name());
        assertThat(result.getPhoto()).isEqualTo(profilePhoto);
        assertThat(result.getIsPrivateAccount()).isEqualTo(user.getProfileSettings().getIsPrivateAccount());
        assertThat(result.getBio()).isEqualTo(user.getProfileSettings().getBio());
        assertThat(result.getSettings()).isNotNull();
        assertThat(result.getSettings().getId()).isEqualTo(user.getProfileSettings().getId());
        
        verify(photoService).getPhotoById(photoId);
    }

    @Test
    void toProfileResponse_shouldHandleNullPhotoId() {
        // Given
        user.setPhotoId(null);

        // When
        UserResponse result = userMapper.toProfileResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isNull();
        verify(photoService, never()).getPhotoById(any());
    }

    @Test
    void toProfileResponse_shouldIncludeSettings() {
        // Given
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toProfileResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSettings()).isNotNull();
        assertThat(result.getSettings().getIsPrivateAccount()).isEqualTo(user.getProfileSettings().getIsPrivateAccount());
        assertThat(result.getSettings().getBio()).isEqualTo(user.getProfileSettings().getBio());
    }

    @Test
    void toSettingsResponse_shouldConvertUserToSettingsResponse() {
        // When
        ProfileSettingsResponse result = userMapper.toSettingsResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getProfileSettings().getId());
        assertThat(result.getBio()).isEqualTo(user.getProfileSettings().getBio());
        assertThat(result.getIsPrivateAccount()).isEqualTo(user.getProfileSettings().getIsPrivateAccount());
        assertThat(result.getIsNotificationsMuted()).isEqualTo(user.getProfileSettings().getIsNotificationsMuted());
        assertThat(result.getIsAllowPublicGroupChatInvite()).isEqualTo(user.getProfileSettings().getIsAllowPublicGroupChatInvite());
        assertThat(result.getIsAllowPublicMessageRequests()).isEqualTo(user.getProfileSettings().getIsAllowPublicMessageRequests());
        assertThat(result.getIsAllowPublicComments()).isEqualTo(user.getProfileSettings().getIsAllowPublicComments());
        assertThat(result.getIsHideLikeCounts()).isEqualTo(user.getProfileSettings().getIsHideLikeCounts());
    }

    @Test
    void toSettingsResponse_shouldConvertBlockedUsersToIds() {
        // Given
        User blockedUser1 = User.builder()
                .id(UUID.randomUUID())
                .username("blocked1")
                .build();
        User blockedUser2 = User.builder()
                .id(UUID.randomUUID())
                .username("blocked2")
                .build();
        
        user.getProfileSettings().setBlockedUsers(List.of(blockedUser1, blockedUser2));

        // When
        ProfileSettingsResponse result = userMapper.toSettingsResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBlockedUserIds()).hasSize(2);
        assertThat(result.getBlockedUserIds()).containsExactly(blockedUser1.getId(), blockedUser2.getId());
    }

    @Test
    void toSettingsResponse_shouldHandleEmptyBlockedUsers() {
        // Given
        user.getProfileSettings().setBlockedUsers(List.of());

        // When
        ProfileSettingsResponse result = userMapper.toSettingsResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBlockedUserIds()).isEmpty();
    }

    @Test
    void toSettingsResponse_shouldHandleNullBlockedUsers() {
        // Given
        user.getProfileSettings().setBlockedUsers(null);

        // When
        ProfileSettingsResponse result = userMapper.toSettingsResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBlockedUserIds()).isNull();
    }

    @Test
    void getProfilePhoto_shouldReturnNullWhenPhotoIdIsNull() {
        // Given
        user.setPhotoId(null);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result.getPhoto()).isNull();
        verify(photoService, never()).getPhotoById(any());
    }

    @Test
    void getProfilePhoto_shouldCallPhotoServiceWhenPhotoIdExists() {
        // Given
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result.getPhoto()).isNotNull();
        verify(photoService).getPhotoById(photoId);
    }

    @Test
    void toResponse_shouldHandleAdminRole() {
        // Given
        user.setRole(Role.ADMIN);
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void toResponse_shouldHandleNullProfileSettings() {
        // Given
        user.setProfileSettings(null);
        when(photoService.getPhotoById(photoId)).thenReturn(profilePhoto);

        // When
        UserResponse result = userMapper.toResponse(user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsPrivateAccount()).isNull();
        assertThat(result.getBio()).isNull();
    }
}
