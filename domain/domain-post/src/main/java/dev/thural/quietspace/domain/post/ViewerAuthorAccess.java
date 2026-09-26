package dev.thural.quietspace.domain.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.UUID;

/**
 * Denormalized follow edge for feed visibility (see ADR 005).
 *
 * <p>Row (viewer, author) means the viewer follows the author and may see the
 * author's private posts. Maintained by {@code PostVisibilityProjector} from
 * follow/unfollow lifecycle events.</p>
 */
@Entity
@IdClass(ViewerAuthorAccess.ViewerAuthorAccessId.class)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ViewerAuthorAccess {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "viewer_id", columnDefinition = "varchar(36)")
    private UUID viewerId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "author_id", columnDefinition = "varchar(36)")
    private UUID authorId;

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewerAuthorAccessId implements Serializable {

        private UUID viewerId;
        private UUID authorId;
    }
}
