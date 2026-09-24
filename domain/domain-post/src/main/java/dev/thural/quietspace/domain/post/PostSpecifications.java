package dev.thural.quietspace.post;

import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.user.ProfileSettings;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostSpecifications {

    private final UserService userService;

    public Specification<Post> visibleToUser() {
        return (root, query, criteriaBuilder) -> {
            User signedUser = userService.getSignedUser();
            Join<Post, User> userJoin = root.join("user");
            Join<User, ProfileSettings> settingsJoin = userJoin.join("profileSettings");

            Predicate publicAccount = criteriaBuilder.equal(settingsJoin.get("isPrivateAccount"), false);
            Predicate isFollower = criteriaBuilder.isMember(signedUser, userJoin.get("followers"));
            Predicate isOwner = criteriaBuilder.equal(userJoin.get("id"), signedUser.getId());

            return criteriaBuilder.or(publicAccount, isFollower, isOwner);
        };
    }

    public Specification<Post> containsText(String searchText) {
        return (root, query, criteriaBuilder) -> {

            if (!StringUtils.hasText(searchText)) return null;

            String likePattern = "%" + searchText.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            likePattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("text")),
                            likePattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("user").get("username")),
                            likePattern
                    )
            );
        };
    }

    public Specification<Post> commentedByUser(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Post, Comment> commentJoin = root.join("comments");

            return criteriaBuilder.equal(commentJoin.get("user").get("id"), userId);
        };
    }

    public Specification<Post> commentedBySignedUser() {
        return commentedByUser(userService.getSignedUser().getId());
    }

    public Specification<Post> savedByUser(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            Join<Post, User> savedByJoin = root.join("savedByUsers");
            return criteriaBuilder.equal(savedByJoin.get("id"), userId);
        };
    }

    public Specification<Post> byUser(User user) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user"), user);
    }
}

