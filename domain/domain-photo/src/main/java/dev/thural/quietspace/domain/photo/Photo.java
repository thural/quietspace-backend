package dev.thural.quietspace.domain.photo;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.core.shared.enums.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
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
@AllArgsConstructor
@NoArgsConstructor
public class Photo extends BaseEntity {
    private String name;
    private String type;
    @Lob
    private byte[] data;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entity_id", columnDefinition = "varchar(36)")
    private UUID entityId;
    private EntityType entityType;
}
