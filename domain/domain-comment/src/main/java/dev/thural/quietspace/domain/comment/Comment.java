package dev.thural.quietspace.domain.comment;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
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
public class Comment extends BaseEntity implements Serializable {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "parent_id", columnDefinition = "varchar(36)")
    private UUID parentId;

    @NotBlank
    @Column(length = 999)
    private String text;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID userId;

    @NotNull
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "post_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID postId;

}
