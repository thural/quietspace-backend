package dev.thural.quietspace.domain.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Denormalized feed-visibility read model (see ADR 005).
 *
 * <p>One row per author, maintained by {@code PostVisibilityProjector} from user
 * lifecycle events. Feed queries join this local table instead of the user
 * aggregate so pagination stays dense. Eventual consistency window accepted.</p>
 */
@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PostAuthorVisibility {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "author_id", columnDefinition = "varchar(36)")
    private UUID authorId;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;
}
