package dev.thural.quietspace.domain.post;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.List;
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

    @JsonIgnore    private UUID photoId;

    @NotNull
    @ManyToOne
    @JsonBackReference
    private User user;

    @JsonIgnore
    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Poll poll;

    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private List<Comment> comments;

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
    public static Post repostBy(User user, String text, String originalPostId) {
        Post repost = new Post();
        repost.setUser(user);
        repost.setText(text);
        repost.setRepostId(originalPostId);
        return repost;
    }
}
