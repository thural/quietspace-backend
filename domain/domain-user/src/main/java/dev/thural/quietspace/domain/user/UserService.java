package dev.thural.quietspace.user;

import dev.thural.quietspace.shared.enums.StatusType;
import dev.thural.quietspace.user.dto.ProfileSettingsRequest;
import dev.thural.quietspace.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.user.dto.UserRequest;
import dev.thural.quietspace.user.dto.UserResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Page<UserResponse> listUsers(Integer pageNumber, Integer pageSize);

    Page<UserResponse> listUsersByUsername(String username, Integer pageNumber, Integer pageSize);

    List<User> getUsersFromIdList(List<UUID> userIds);

    Optional<UserResponse> getUserResponseById(UUID id);

    void deleteUserById(UUID userId);

    UserResponse patchUser(UserRequest userRequest);

    Optional<UserResponse> getLoggedUserResponse();

    Page<UserResponse> listFollowings(UUID userId, Integer pageSize, Integer pageNumber);

    Page<UserResponse> listFollowers(UUID userId, Integer pageNumber, Integer pageSize);

    void toggleFollow(UUID followedUserId);

    void followUser(UUID userId);

    void unfollowUser(UUID userId);

    User getSignedUser();

    void removeFollower(UUID followingUserId);

    void setOnlineStatus(String userEmail, StatusType type);

    List<UserResponse> findConnectedFollowings();

    Page<UserResponse> queryUsers(String username, String firstname, String lastname, Integer pageNumber, Integer pageSize);

    Optional<User> getUserById(UUID memberId);

    ProfileSettingsResponse saveProfileSettings(ProfileSettingsRequest request);

    void addUserToBlockList(UUID userId);

    void removeUserFromBlockList(UUID userId);

    List<UserResponse> getBlockedUsers();

    void disableUser(UUID userId);
}
