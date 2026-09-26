package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.comment.api.CommentQueryPort;
import dev.thural.quietspace.domain.user.ProfileSettings;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
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
    private final UserQueryPort userQueryPort;
    private final CommentQueryPort commentQueryPort;

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

            // Author-username matching goes through the user query port so the
            // database filter stays an IN predicate (pagination-safe, no join).
            // An empty id set must yield no rows: render an always-false predicate
            // instead of an empty IN list.
            var authorIds = userQueryPort.searchUserIds(searchText);
            var authorMatch = authorIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("authorId").in(authorIds);

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            likePattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("text")),
                            likePattern
                    ),
                    authorMatch
            );
        };
    }

    public Specification<Post> commentedByUser(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            // Port-resolved id set keeps filtering in SQL (pagination-safe, no join).
            var postIds = commentQueryPort.findPostIdsByUserId(userId);
            return postIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("id").in(postIds);
        };
    }

    public Specification<Post> commentedBySignedUser() {
        return commentedByUser(userService.getSignedUser().getId());
    }

    public Specification<Post> savedWithIds(java.util.List<UUID> savedIds) {
        return (root, query, criteriaBuilder) -> root.get("id").in(savedIds);
    }

    public Specification<Post> byUser(User user) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user"), user);
    }
}

