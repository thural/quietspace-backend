package dev.thural.quietspace.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

    Page<Notification> findAllByUserIdAndNotificationType(UUID userId, NotificationType type, Pageable pageable);

    Optional<Notification> findByContentIdAndUserId(UUID contentId, UUID id);

    Integer countByUserIdAndIsSeen(UUID contentId, Boolean isSeen);

}