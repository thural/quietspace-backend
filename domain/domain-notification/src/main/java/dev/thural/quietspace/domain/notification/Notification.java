package dev.thural.quietspace.notification;
import dev.thural.quietspace.shared.entity.BaseEntity;

import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.notification.NotificationType;
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

    @NotNull
    private UUID userId;

    @NotNull
    private UUID actorId;

    @NotNull
    private UUID contentId;

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
