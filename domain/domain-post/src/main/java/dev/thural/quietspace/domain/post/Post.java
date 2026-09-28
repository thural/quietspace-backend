package dev.thural.quietspace.domain.post;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Post extends BaseEntity implements Serializable {

    private String title;
    private String repostText;
    private String repostId;

    @Length(min = 1, max = 999)
    private String text;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "photo_id", columnDefinition = "varchar(36)")
    private UUID photoId;

    @NotNull
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Transitional read-only view of the {@code user_id} FK column (Phase E.3).
     * New code must use this id instead of navigating the {@code user} association;
     * the association stays as the single writer until feed-privacy specifications
     * ({@code PostSpecifications.visibleToUser}) can be redesigned without SQL joins.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", insertable = false, updatable = false, columnDefinition = "varchar(36)")
    private UUID authorId;

    @JsonIgnore
    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Poll poll;

    // Domain methods for poll voting
    public void votePoll(UUID userId, String optionLabel) {
        if (poll == null) {
            throw new IllegalStateException("Post does not have a poll");
        }
        PollOption option = poll.getOptions().stream()
                .filter(opt -> opt.getLabel().equals(optionLabel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid poll option"));
        
        // Check if user already voted on any option (silently return if already voted)
        boolean alreadyVoted = poll.getOptions().stream()
                .anyMatch(opt -> opt.getVotes().contains(userId));
        if (alreadyVoted) {
            return;
        }
        
        option.getVotes().add(userId);
    }

    // Factory method for repost
    public static Post repostBy(UUID authorId, String text, String originalPostId) {
        Post repost = new Post();
        repost.setAuthorId(authorId);
        repost.setText(text);
        repost.setRepostId(originalPostId);
        return repost;
    }
}
