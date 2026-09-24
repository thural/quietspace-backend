package dev.thural.quietspace.user.controller;

import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin:read')")
    ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("hello admin");
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin:delete')")
    ResponseEntity<Void> deleteUserById(@PathVariable UUID userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('admin:delete')")
    ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin:read')")
    ResponseEntity<Page<UserResponse>> getPagedUsers(@RequestParam Integer pageNumber, @RequestParam Integer pageSize) {
        return ResponseEntity.ok(userService.listUsers(pageNumber, pageSize));
    }

    @PutMapping("/users/{userId}/disable")
    @PreAuthorize("hasAuthority('admin:update')")
    ResponseEntity<Void> disableUser(@PathVariable UUID userId) {
        userService.disableUser(userId);
        return ResponseEntity.ok().build();
    }

}
