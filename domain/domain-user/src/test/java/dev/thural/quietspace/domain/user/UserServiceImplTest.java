package dev.thural.quietspace.user;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.shared.enums.StatusType;
import dev.thural.quietspace.shared.exception.CustomErrorException;
import dev.thural.quietspace.shared.util.PagingProvider;
import dev.thural.quietspace.user.*;
import dev.thural.quietspace.user.dto.ProfileSettingsRequest;
import dev.thural.quietspace.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.user.dto.UserRequest;
import dev.thural.quietspace.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    Authentication authentication;
    @Mock
    SecurityContext securityContext;

    @Mock
    private UserMapper userMapper;
    @Mock
    private PhotoService photoService;
    @Mock
    private UserQuery userQuery;

    @InjectMocks
    UserServiceImpl userService;

    private UUID userId;
    private User user;
    private UserRequest registerRequest;
    private ProfileSettings profileSettings;
    private User followingUser;
    private User followerUser;

    @BeforeEach
    void initMockData() {
        this.userId = UUID.fromString("e18d0c0c-37a4-4e50-8041-bd49ffde8182");

        this.user = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .role(Role.USER)
                .build();

        this.registerRequest = UserRequest.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .role(user.getRole().name())
                .password(user.getPassword())
                .build();

        lenient().when(authentication.getName()).thenReturn("user");
        lenient().when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        lenient().when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse());
        lenient().when(userMapper.toProfileResponse(any(User.class))).thenReturn(new UserResponse());
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User foundUser = userService.getUserById(userId).orElseThrow(null);

        assertThat(foundUser).isInstanceOf(User.class);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserResponseById_shouldReturnUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserResponse foundUser = userService.getUserResponseById(userId).orElseThrow();

        assertThat(foundUser).isInstanceOf(UserResponse.class);
        verify(userRepository, never()).findById(userId);
    }

    @Test
    void listAll_shouldReturnUsers() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(1, 50, null);
        when(userRepository.findAll(pageRequest)).thenReturn(Page.empty());

        Page<UserResponse> userPage = userService.listUsers(1, 50);

        assertThat(userPage).isEmpty();
        verify(userRepository, times(1)).findAll(pageRequest);
    }

    @Test
    void getUsersFromIdList_shouldReturnUsers() {
        List<UUID> userIdList = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));

        List<User> userList = userService.getUsersFromIdList(userIdList);

        assertThat(userList).isNotEmpty();
        verify(userRepository, times(2)).findById(any(UUID.class));
    }

    @Test
    void getSignedUser_shouldReturnUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));

        User loggedUser = userService.getSignedUser();

        assertThat(loggedUser).isInstanceOf(User.class);
        verify(userRepository, times(1)).findUserByUsername(any());
    }

    @Test
    void getSignedUserResponse_shouldReturnUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));

        UserResponse loggedUser = userService.getLoggedUserResponse().orElse(null);

        assertThat(loggedUser).isInstanceOf(UserResponse.class);
        verify(userRepository, times(1)).findUserByUsername(any());
    }

    @Test
    void deleteUser_shouldSucceed() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);

        userService.deleteUserById(userId);

        verify(userRepository, times(1)).findUserByUsername(any());
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void updateUser_shouldReturnUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        UserResponse userResponse = userService.patchUser(registerRequest);

        assertThat(userResponse).isInstanceOf(UserResponse.class);
        verify(userRepository, times(1)).findUserByUsername(any());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void queryUsers_givenUsernameAndPagination_shouldReturnPage() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(0, 10, null);
        when(userQuery.findAllByQuery("testuser", null, null, pageRequest)).thenReturn(Page.empty());

        Page<UserResponse> result = userService.queryUsers("testuser", null, null, 0, 10);

        assertThat(result).isEmpty();
        verify(userQuery).findAllByQuery("testuser", null, null, pageRequest);
    }

    @Test
    void listUsersByUsername_givenSearchTerm_shouldReturnMatchingPage() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(0, 10, null);
        when(userRepository.findAllBySearchTerm("testuser", pageRequest)).thenReturn(Page.empty());

        Page<UserResponse> result = userService.listUsersByUsername("testuser", 0, 10);

        assertThat(result).isEmpty();
        verify(userRepository).findAllBySearchTerm("testuser", pageRequest);
    }

    @Test
    void listUsersByUsername_givenBlankSearchTerm_shouldReturnAll() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(0, 10, null);
        when(userRepository.findAll(pageRequest)).thenReturn(Page.empty());

        Page<UserResponse> result = userService.listUsersByUsername("", 0, 10);

        assertThat(result).isEmpty();
        verify(userRepository).findAll(pageRequest);
    }

    @Test
    void listFollowings_givenPublicProfile_shouldReturnPage() {
        user.setFollowings(List.of(User.builder().id(UUID.randomUUID()).build()));
        profileSettings = ProfileSettings.builder().isPrivateAccount(false).build();
        user.setProfileSettings(profileSettings);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse());
        SecurityContextHolder.setContext(securityContext);

        Page<UserResponse> result = userService.listFollowings(userId, 10, 0);

        assertThat(result).hasSize(1);
    }

    @Test
    void toggleFollow_givenNotCurrentlyFollowing_shouldAdd() {
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.toggleFollow(target.getId());

        assertThat(signedUser.getFollowings()).contains(target);
        assertThat(target.getFollowers()).contains(signedUser);
    }

    @Test
    void toggleFollow_givenCurrentlyFollowing_shouldRemove() {
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        signedUser.getFollowings().add(target);
        target.getFollowers().add(signedUser);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.toggleFollow(target.getId());

        assertThat(signedUser.getFollowings()).doesNotContain(target);
        assertThat(target.getFollowers()).doesNotContain(signedUser);
    }

    @Test
    void toggleFollow_givenSelfTarget_shouldThrow() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> userService.toggleFollow(signedUser.getId()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("can't unfollow themselves");
    }

    @Test
    void removeFollower_givenValidFollower_shouldRemove() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User follower = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        signedUser.getFollowers().add(follower);
        follower.getFollowings().add(signedUser);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.removeFollower(follower.getId());

        assertThat(signedUser.getFollowers()).doesNotContain(follower);
        assertThat(follower.getFollowings()).doesNotContain(signedUser);
    }

    @Test
    void removeFollower_givenNotAFollower_shouldThrow() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User nonFollower = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(nonFollower.getId())).thenReturn(Optional.of(nonFollower));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> userService.removeFollower(nonFollower.getId()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("not found in followers");
    }

    @Test
    void removeFollower_givenSelfTarget_shouldThrow() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> userService.removeFollower(signedUser.getId()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("can't unfollow themselves");
    }

    @Test
    void setOnlineStatus_givenExistingEmail_shouldSetUserOffline() {
        when(userRepository.findUserEntityByEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.setOnlineStatus("test@example.com", StatusType.OFFLINE);

        assertThat(user.getStatusType()).isEqualTo(StatusType.OFFLINE);
    }

    @Test
    void findConnectedFollowings_givenOnlineFollowings_shouldReturnList() {
        User signedUser = User.builder().id(UUID.randomUUID()).followings(new ArrayList<>()).followings(new ArrayList<>()).build();
        User onlineFollowing = User.builder().statusType(StatusType.ONLINE).build();
        User offlineFollowing = User.builder().statusType(StatusType.OFFLINE).build();
        signedUser.getFollowings().add(onlineFollowing);
        signedUser.getFollowings().add(offlineFollowing);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userMapper.toResponse(onlineFollowing)).thenReturn(new UserResponse());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        List<UserResponse> result = userService.findConnectedFollowings();

        assertThat(result).hasSize(1);
    }

    @Test
    void saveProfileSettings_givenValidRequest_shouldCopyAndReturnResponse() {
        ProfileSettings settings = ProfileSettings.builder().build();
        user.setProfileSettings(settings);
        ProfileSettingsRequest request = ProfileSettingsRequest.builder().bio("new bio").build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userMapper.toSettingsResponse(user)).thenReturn(new ProfileSettingsResponse());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        ProfileSettingsResponse result = userService.saveProfileSettings(request);

        assertThat(result).isInstanceOf(ProfileSettingsResponse.class);
        assertThat(user.getProfileSettings().getBio()).isEqualTo("new bio");
    }

    @Test
    void followUser_shouldFollowUser() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.followUser(target.getId());

        assertThat(signedUser.getFollowings()).contains(target);
        assertThat(target.getFollowers()).contains(signedUser);
    }

    @Test
    void followUser_givenAlreadyFollowing_shouldBeNoOp() {
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        signedUser.getFollowings().add(target);
        target.getFollowers().add(signedUser);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.followUser(target.getId());

        assertThat(signedUser.getFollowings()).hasSize(1);
        assertThat(target.getFollowers()).hasSize(1);
    }

    @Test
    void followUser_givenSelf_shouldThrow() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> userService.followUser(signedUser.getId()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("cannot follow yourself");
    }

    @Test
    void unfollowUser_shouldUnfollowUser() {
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        signedUser.getFollowings().add(target);
        target.getFollowers().add(signedUser);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.unfollowUser(target.getId());

        assertThat(signedUser.getFollowings()).doesNotContain(target);
        assertThat(target.getFollowers()).doesNotContain(signedUser);
    }

    @Test
    void unfollowUser_givenNotFollowing_shouldBeNoOp() {
        User target = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.unfollowUser(target.getId());

        assertThat(signedUser.getFollowings()).doesNotContain(target);
    }

    @Test
    void unfollowUser_givenSelf_shouldThrow() {
        User signedUser = User.builder().id(UUID.randomUUID()).followers(new ArrayList<>()).followings(new ArrayList<>()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(signedUser));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThatThrownBy(() -> userService.unfollowUser(signedUser.getId()))
                .isInstanceOf(CustomErrorException.class)
                .hasMessageContaining("cannot unfollow yourself");
    }

    @Test
    void addUserToBlockList_givenExistingUser_shouldAddToBlocked() {
        ProfileSettings settings = ProfileSettings.builder().blockedUsers(new ArrayList<>()).build();
        user.setProfileSettings(settings);
        User target = User.builder().id(UUID.randomUUID()).build();
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.addUserToBlockList(target.getId());

        assertThat(user.getProfileSettings().getBlockedUsers()).contains(target);
    }

    @Test
    void removeUserFromBlockList_shouldRemoveBlockedUser() {
        User target = User.builder().id(UUID.randomUUID()).build();
        ProfileSettings settings = ProfileSettings.builder().blockedUsers(new ArrayList<>(List.of(target))).build();
        user.setProfileSettings(settings);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.removeUserFromBlockList(target.getId());

        assertThat(user.getProfileSettings().getBlockedUsers()).doesNotContain(target);
    }

    @Test
    void removeUserFromBlockList_givenNotBlocked_shouldBeNoOp() {
        User target = User.builder().id(UUID.randomUUID()).build();
        ProfileSettings settings = ProfileSettings.builder().blockedUsers(new ArrayList<>()).build();
        user.setProfileSettings(settings);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        userService.removeUserFromBlockList(target.getId());

        assertThat(user.getProfileSettings().getBlockedUsers()).doesNotContain(target);
    }

    @Test
    void getBlockedUsers_shouldReturnList() {
        User blocked = User.builder().id(UUID.randomUUID()).build();
        ProfileSettings settings = ProfileSettings.builder().blockedUsers(new ArrayList<>(List.of(blocked))).build();
        user.setProfileSettings(settings);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(userMapper.toResponse(blocked)).thenReturn(new UserResponse());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        List<UserResponse> result = userService.getBlockedUsers();

        assertThat(result).hasSize(1);
    }

    @Test
    void getBlockedUsers_givenNone_shouldReturnEmpty() {
        ProfileSettings settings = ProfileSettings.builder().blockedUsers(new ArrayList<>()).build();
        user.setProfileSettings(settings);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.of(user));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        List<UserResponse> result = userService.getBlockedUsers();

        assertThat(result).isEmpty();
    }

    @Test
    void disableUser_shouldSetEnabledToFalse() {
        User target = User.builder().id(UUID.randomUUID()).enabled(true).build();
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        userService.disableUser(target.getId());

        verify(userRepository).save(target);
        assertThat(target.isEnabled()).isFalse();
    }

    @Test
    void disableUser_givenNonExistent_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.disableUser(id))
                .isInstanceOf(dev.thural.quietspace.shared.exception.UserNotFoundException.class);
    }

}