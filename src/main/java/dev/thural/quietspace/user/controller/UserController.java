package dev.thural.quietspace.user.controller;

import dev.thural.quietspace.notification.NotificationService;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.ProfileSettingsRequest;
import dev.thural.quietspace.user.dto.ProfileSettingsResponse;
import dev.thural.quietspace.user.dto.UserRequest;
import dev.thural.quietspace.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static dev.thural.quietspace.notification.NotificationType.FOLLOW_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyAuthority('user:read', 'admin:read')")
public class UserController {

    public static final String USER_PATH = "/api/v1/users";
    public static final String USER_PATH_ID = "/{userId}";
    public static final String FOLLOW_PATH_ID = "/follow/{userId}";
    public static final String FOLLOW_USER_TOGGLE_PATH = FOLLOW_PATH_ID + "/toggle-follow";

    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/search")
    Page<UserResponse> listUsersBySearchTerm(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return userService.listUsersByUsername(username, pageNumber, pageSize);
    }

    @GetMapping("/query")
    Page<UserResponse> listUsersByQuery(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "firstname", required = false) String firstname,
            @RequestParam(name = "lastname", required = false) String lastname,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return userService.queryUsers(username, firstname, lastname, pageNumber, pageSize);
    }

    @GetMapping(USER_PATH_ID)
    ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return userService.getUserResponseById(userId)
                .map(user -> ResponseEntity.ok().headers(headers).body(user))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(USER_PATH_ID)
    ResponseEntity<?> deleteUser(@PathVariable UUID userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    ResponseEntity<UserResponse> patchMe(@RequestBody @Valid UserRequest userRequest) {
        return ResponseEntity.ok(userService.patchUser(userRequest));
    }

    @PatchMapping
    ResponseEntity<UserResponse> patchUser(@RequestBody @Valid UserRequest userRequest) {
        return ResponseEntity.ok(userService.patchUser(userRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        return userService.getLoggedUserResponse()
                .map(profile -> ResponseEntity.ok().body(profile))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserFromToken() {
        return userService.getLoggedUserResponse()
                .map(profile -> ResponseEntity.ok().body(profile))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/profile/block/{userId}")
    public ResponseEntity<Void> blockUserProfile(@PathVariable UUID userId) {
        userService.addUserToBlockList(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/profile/block/{userId}")
    public ResponseEntity<Void> unblockUserProfile(@PathVariable UUID userId) {
        userService.removeUserFromBlockList(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/online")
    public ResponseEntity<List<UserResponse>> getOnlineUsers() {
        return ResponseEntity.ok(userService.findConnectedFollowings());
    }

    @GetMapping("/profile/blocked")
    public ResponseEntity<List<UserResponse>> getBlockedUsers() {
        return ResponseEntity.ok(userService.getBlockedUsers());
    }

    @PatchMapping("/profile/settings")
    public ResponseEntity<ProfileSettingsResponse> saveSettings(@RequestBody ProfileSettingsRequest request) {
        return ResponseEntity.ok(userService.saveProfileSettings(request));
    }

    @PostMapping("/{userId}/follow")
    ResponseEntity<Void> followUser(@PathVariable UUID userId) {
        userService.followUser(userId);
        notificationService.processNotification(FOLLOW_REQUEST, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/follow")
    ResponseEntity<Void> unfollowUser(@PathVariable UUID userId) {
        userService.unfollowUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(FOLLOW_USER_TOGGLE_PATH)
    ResponseEntity<?> toggleFollow(@PathVariable UUID userId) {
        userService.toggleFollow(userId);
        notificationService.processNotification(FOLLOW_REQUEST, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/followers/remove/{userId}")
    ResponseEntity<?> removeFollower(@PathVariable UUID userId) {
        userService.removeFollower(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/followers/{followerId}")
    ResponseEntity<Void> removeFollowerById(@PathVariable UUID userId, @PathVariable UUID followerId) {
        userService.removeFollower(followerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followings")
    Page<UserResponse> listFollowings(
            @PathVariable UUID userId,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return userService.listFollowings(userId, pageSize, pageNumber);
    }

    @GetMapping("/{userId}/followers")
    Page<UserResponse> listFollowers(
            @PathVariable UUID userId,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return userService.listFollowers(userId, pageNumber, pageSize);
    }

}
