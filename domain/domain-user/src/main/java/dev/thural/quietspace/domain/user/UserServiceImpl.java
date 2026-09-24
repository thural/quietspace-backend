package dev.thural.quietspace.domain.user;

import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.core.shared.enums.Role;
import dev.thural.quietspace.core.shared.event.TransactionalEventPublisher;
import dev.thural.quietspace.core.shared.event.UserFollowedEvent;
import dev.thural.quietspace.core.shared.enums.StatusType;
import dev.thural.quietspace.core.shared.exception.CustomErrorException;
import dev.thural.quietspace.core.shared.exception.UnauthorizedException;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.core.shared.util.PageUtils;
import dev.thural.quietspace.core.shared.util.PagingProvider;
import dev.thural.quietspace.domain.user.dto.ProfileSettingsRequest;
import dev.thural.quietspace.domain.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.domain.user.dto.UserRequest;
import dev.thural.quietspace.domain.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.shared.enums.StatusType.OFFLINE;
import static dev.thural.quietspace.core.shared.enums.StatusType.ONLINE;
import static dev.thural.quietspace.core.shared.util.PagingProvider.DEFAULT_SORT_OPTION;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserQuery userQuery;
    private final PhotoService photoService;
    private final TransactionalEventPublisher eventPublisher;

    @Override
    @Transactional
    public Page<UserResponse> listUsers(Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PagingProvider.buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return userRepository.findAll(pageRequest).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public Page<UserResponse> queryUsers(String username, String firstname, String lastname, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PagingProvider.buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return userQuery.findAllByQuery(username, firstname, lastname, pageRequest).map(userMapper::toResponse);
    }

    @Override
    public Optional<User> getUserById(UUID memberId) {
        return userRepository.findById(memberId);
    }

    @Override
    @Transactional
    public ProfileSettingsResponse saveProfileSettings(ProfileSettingsRequest request) {
        User signedUser = getSignedUser();
        BeanUtils.copyProperties(request, signedUser.getProfileSettings());
        return userMapper.toSettingsResponse(signedUser);
    }

    @Override
    @Transactional
    public void addUserToBlockList(UUID userId) {
        User signedUser = getSignedUser();
        User requestedUser = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        signedUser.block(requestedUser);
    }

    @Override
    @Transactional
    public void removeUserFromBlockList(UUID userId) {
        User signedUser = getSignedUser();
        User requestedUser = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        signedUser.unblock(requestedUser);
    }

    @Override
    @Transactional
    public void disableUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    public List<UserResponse> getBlockedUsers() {
        User signedUser = getSignedUser();
        return signedUser.getProfileSettings().getBlockedUsers().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public Page<UserResponse> listUsersByUsername(String username, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = PagingProvider.buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        if (StringUtils.hasText(username)) {
            return userRepository.findAllBySearchTerm(username, pageRequest).map(userMapper::toResponse);
        } else {
            return userRepository.findAll(pageRequest).map(userMapper::toResponse);
        }
    }

    @Override
    public Optional<UserResponse> getLoggedUserResponse() {
        UserResponse userResponse = userMapper.toProfileResponse(getSignedUser());
        return Optional.of(userResponse);
    }

    @Override
    public User getSignedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new UnauthorizedException("no authenticated user");
        String username = authentication.getName();
        return userRepository.findUserByUsername(username).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public List<User> getUsersFromIdList(List<UUID> userIds) {
        // NOTE: mutable ArrayList — callers attach the result to entities
        // (e.g. Chat members) and mutate it later within a persistence context.
        return userIds.stream().map(userId -> userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    @Override
    public Optional<UserResponse> getUserResponseById(UUID userId) {
        User user = checkUserProfileAccessAndReturnUser(userId);
        return Optional.of(user).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteUserById(UUID userId) {
        User signedUser = getSignedUser();
        boolean hasAdminRole = isHasAdminRole(signedUser);
        if (!hasAdminRole && !signedUser.getId().equals(userId))
            throw new UnauthorizedException("user denied to delete resource");
        userRepository.deleteById(userId);
        photoService.deletePhotoByEntityId(userId);
    }

    @Override
    public UserResponse patchUser(UserRequest userRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("granted authorities in patchUser: {}", auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).reduce(" ", (a, b) -> a + b));
        }
        User signedUser = getSignedUser();
        boolean hasAdminRole = isHasAdminRole(signedUser);
        if (!hasAdminRole && !userRequest.getEmail().equals(signedUser.getEmail()))
            throw new UnauthorizedException("signed user has no access to requested resource");
        BeanUtils.copyProperties(userRequest, signedUser);
        return userMapper.toResponse(userRepository.save(signedUser));
    }

    private boolean isHasAdminRole(User signedUser) {
        return signedUser.getRole() == Role.ADMIN;
    }

    private User checkUserProfileAccessAndReturnUser(UUID userId) {
        User signedUser = getSignedUser();
        if (signedUser.getId().equals(userId)) return signedUser;
        User user = getUserById(userId).orElseThrow(UserNotFoundException::new);
        if (!user.getProfileSettings().getIsPrivateAccount()) return user;
        if (user.getFollowers().contains(getSignedUser())) return user;
        throw new UnauthorizedException("signed user has no access to requested resource");
    }

    @Override
    @Transactional
    public Page<UserResponse> listFollowings(UUID userId, Integer pageSize, Integer pageNumber) {
        User user = checkUserProfileAccessAndReturnUser(userId);
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        Page<User> userPage = PageUtils.pageFromList(user.getFollowings(), pageRequest);
        return userPage.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public Page<UserResponse> listFollowers(UUID userId, Integer pageNumber, Integer pageSize) {
        User user = checkUserProfileAccessAndReturnUser(userId);
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        Page<User> userPage = PageUtils.pageFromList(user.getFollowers(), pageRequest);
        return userPage.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void toggleFollow(UUID followedUserId) {
        User signedUser = getSignedUser();
        if (signedUser.getId().equals(followedUserId))
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "users can't unfollow themselves");
        User followedUser = userRepository.findById(followedUserId).orElseThrow(UserNotFoundException::new);
        if (signedUser.getFollowings().contains(followedUser)) {
            signedUser.unfollow(followedUser);
        } else {
            signedUser.follow(followedUser);
        }
        eventPublisher.publish(new UserFollowedEvent(signedUser.getId(), followedUserId));
    }

    @Override
    @Transactional
    public void followUser(UUID userId) {
        User signedUser = getSignedUser();
        if (signedUser.getId().equals(userId))
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "cannot follow yourself");
        User target = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        signedUser.follow(target);
        eventPublisher.publish(new UserFollowedEvent(signedUser.getId(), userId));
    }

    @Override
    @Transactional
    public void unfollowUser(UUID userId) {
        User signedUser = getSignedUser();
        if (signedUser.getId().equals(userId))
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "cannot unfollow yourself");
        User target = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        signedUser.unfollow(target);
    }

    @Override
    @Transactional
    public void removeFollower(UUID followingUserId) {
        User signedUser = getSignedUser();
        if (signedUser.getId().equals(followingUserId))
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "users can't unfollow themselves");
        User followingUser = userRepository.findById(followingUserId).orElseThrow(UserNotFoundException::new);
        if (!signedUser.getFollowers().contains(followingUser)) {
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "user is not found in followers");
        }
        signedUser.removeFollower(followingUser);
        followingUser.unfollow(signedUser);
    }

    @Override
    public void setOnlineStatus(String userEmail, StatusType type) {
        userRepository.findUserEntityByEmail(userEmail).ifPresent(storedUser -> storedUser.setStatusType(OFFLINE));
    }

    @Override
    public List<UserResponse> findConnectedFollowings() {
        User signedUser = getSignedUser();
        return signedUser.getFollowings()
                .stream()
                .filter(following -> following.getStatusType().equals(ONLINE))
                .map(userMapper::toResponse).toList();
    }

}