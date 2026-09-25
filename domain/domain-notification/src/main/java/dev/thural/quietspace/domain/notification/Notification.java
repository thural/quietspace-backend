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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID userId;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actor_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID actorId;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_id", nullable = false, columnDefinition = "varchar(36)")
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
