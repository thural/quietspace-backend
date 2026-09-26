package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.domain.comment.api.CommentQueryPort;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
            // Feed visibility resolves against the denormalized read model
            // (post_author_visibility, viewer_author_access — see ADR 005) so
            // filtering stays inside SQL and pagination stays dense. Authors
            // without a visibility row default to public.
            UUID viewerId = userService.getSignedUser().getId();

            Subquery<UUID> privateAuthors = query.subquery(UUID.class);
            Root<PostAuthorVisibility> visibility = privateAuthors.from(PostAuthorVisibility.class);
            privateAuthors.select(visibility.get("authorId"))
                    .where(criteriaBuilder.isTrue(visibility.get("isPrivate")));
            Predicate isPublic = criteriaBuilder.not(root.get("authorId").in(privateAuthors));

            Predicate isOwner = criteriaBuilder.equal(root.get("authorId"), viewerId);

            Subquery<UUID> access = query.subquery(UUID.class);
            Root<ViewerAuthorAccess> edge = access.from(ViewerAuthorAccess.class);
            access.select(edge.get("authorId")).where(
                    criteriaBuilder.equal(edge.get("viewerId"), viewerId),
                    criteriaBuilder.equal(edge.get("authorId"), root.get("authorId")));
            Predicate isGranted = criteriaBuilder.exists(access);

            return criteriaBuilder.or(isPublic, isOwner, isGranted);
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
}

