package dev.thural.quietspace.domain.notification;
import dev.thural.quietspace.core.shared.entity.BaseEntity;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @NotNull    private UUID userId;

    @NotNull    private UUID actorId;

    @NotNull    private UUID contentId;

    @NotNull
    private Boolean isSeen;

    @Enumerated(EnumType.STRING)
    private EntityType contentType;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @PrePersist
    void initDefaultValues() {
        setIsSeen(false);
    }

}
