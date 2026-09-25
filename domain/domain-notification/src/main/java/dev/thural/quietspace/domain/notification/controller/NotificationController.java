package dev.thural.quietspace.domain.notification.controller;

import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    public static final String NOTIFICATION_PATH = "/api/v1/notifications";
    public static final String NOTIFICATION_PATH_ID = "/{notificationId}";

    private final NotificationService notificationService;

    @PostMapping("/seen/{contentId}")
    ResponseEntity<?> handleSeen(@PathVariable UUID contentId) {
        notificationService.handleSeen(contentId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    Page<NotificationResponse> getAllNotifications(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize
    ) {
        return notificationService.getAllNotifications(pageNumber, pageSize);
    }

    @GetMapping("/type/{notificationType}")
    Page<NotificationResponse> getNotificationsByType(
            @RequestParam Integer pageNumber,
            @RequestParam Integer pageSize,
            @PathVariable String notificationType
    ) {
        return notificationService.getNotificationsByType(pageNumber, pageSize, notificationType);
    }

    @GetMapping("/count-pending")
    ResponseEntity<Integer> getCountOfPendingNotifications() {
        return ResponseEntity.ok(notificationService.getCountOfPendingNotifications());
    }

    @PutMapping("/{notificationId}/read")
    ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.handleSeen(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/process")
    ResponseEntity<?> processNotification(NotificationType type, UUID contentId) {
        notificationService.processNotification(type, contentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/process-reaction")
    ResponseEntity<?> processNotificationByReaction(@RequestParam EntityType type, @RequestParam UUID contentId) {
        notificationService.processNotificationByReaction(type, contentId);
        return ResponseEntity.ok().build();
    }

}
