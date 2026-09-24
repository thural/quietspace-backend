package dev.thural.quietspace.shared.util;

import dev.thural.quietspace.chat.Chat;
import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.message.Message;
import dev.thural.quietspace.notification.Notification;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.post.Poll;
import dev.thural.quietspace.post.PollOption;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.reaction.Reaction;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.notification.NotificationType;
import dev.thural.quietspace.reaction.ReactionType;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// entity wildcard removed - all entities moved to feature packages

/**
 * Factory class for creating test data entities and objects.
 * This utility helps reduce boilerplate code in tests by providing
 * pre-configured test objects with sensible defaults.
 */
public class TestDataFactory {

    // User creation methods
    public static User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static User createTestUser(String username, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .password("encodedPassword")
                .role(Role.USER)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static User createAdminUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@test.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Chat creation methods
    public static Chat createTestChat(User... users) {
        return Chat.builder()
                .id(UUID.randomUUID())
                .users(List.of(users))
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Message creation methods
    public static Message createTestMessage(Chat chat, User sender, User recipient) {
        return Message.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .sender(sender)
                .recipient(recipient)
                .text("Test message")
                .isSeen(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Post creation methods
    public static Post createTestPost(User user) {
        return Post.builder()
                .id(UUID.randomUUID())
                .title("Test Post")
                .text("This is a test post")
                .user(user)
                .comments(List.of())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static Post createTestPostWithPhoto(User user, UUID photoId) {
        return Post.builder()
                .id(UUID.randomUUID())
                .title("Test Post with Photo")
                .text("This is a test post with photo")
                .user(user)
                .photoId(photoId)
                .comments(List.of())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static Post createTestPostWithPoll(User user) {
        Poll poll = createTestPoll();
        return Post.builder()
                .id(UUID.randomUUID())
                .title("Test Post with Poll")
                .text("This is a test post with poll")
                .user(user)
                .poll(poll)
                .comments(List.of())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Comment creation methods
    public static Comment createTestComment(Post post, User user) {
        return Comment.builder()
                .id(UUID.randomUUID())
                .text("This is a test comment")
                .user(user)
                .post(post)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static Comment createReplyComment(Comment parentComment, Post post, User user) {
        return Comment.builder()
                .id(UUID.randomUUID())
                .parentId(parentComment.getId())
                .text("This is a reply comment")
                .user(user)
                .post(post)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Reaction creation methods
    public static Reaction createTestReaction(User user, UUID contentId, EntityType contentType, ReactionType reactionType) {
        return Reaction.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .username(user.getUsername())
                .contentId(contentId)
                .contentType(contentType)
                .reactionType(reactionType)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static Reaction createLikeReaction(User user, UUID contentId) {
        return createTestReaction(user, contentId, EntityType.POST, ReactionType.LIKE);
    }

    public static Reaction createDislikeReaction(User user, UUID contentId) {
        return createTestReaction(user, contentId, EntityType.POST, ReactionType.DISLIKE);
    }

    // Poll creation methods
    public static Poll createTestPoll() {
        List<PollOption> options = List.of(
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 1")
                        .votes(Set.of(UUID.randomUUID(), UUID.randomUUID()))
                        .build(),
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 2")
                        .votes(Set.of(UUID.randomUUID()))
                        .build(),
                PollOption.builder()
                        .id(UUID.randomUUID())
                        .label("Option 3")
                        .votes(Set.of())
                        .build()
        );
        
        return Poll.builder()
                .id(UUID.randomUUID())
                .dueDate(OffsetDateTime.now().plusDays(7))
                .options(options)
                .build();
    }

    // Notification creation methods
    public static Notification createTestNotification(UUID userId, UUID actorId, UUID contentId, NotificationType type) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .actorId(actorId)
                .contentId(contentId)
                .isSeen(false)
                .contentType(EntityType.POST)
                .notificationType(type)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    public static Notification createSeenNotification(UUID userId, UUID actorId, UUID contentId, NotificationType type) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .actorId(actorId)
                .contentId(contentId)
                .isSeen(true)
                .contentType(EntityType.POST)
                .notificationType(type)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    // Photo response creation methods
    public static PhotoResponse createTestPhotoResponse() {
        return PhotoResponse.builder()
                .id(UUID.randomUUID())
                .name("test.jpg")
                .type("image/jpeg")
                .data(new byte[]{1, 2, 3})
                .build();
    }

    public static PhotoResponse createTestPhotoResponse(UUID id, String name) {
        return PhotoResponse.builder()
                .id(id)
                .name(name)
                .type("image/jpeg")
                .data(new byte[]{1, 2, 3})
                .build();
    }

    // UUID generation helper
    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    // Common test data combinations
    public static class TestDataSet {
        public final User user1;
        public final User user2;
        public final User admin;
        public final Chat chat;
        public final Post post;
        public final Comment comment;
        public final Message message;
        public final Reaction like;
        public final Reaction dislike;
        public final Notification notification;

        public TestDataSet() {
            this.user1 = createTestUser("user1", "user1@test.com");
            this.user2 = createTestUser("user2", "user2@test.com");
            this.admin = createAdminUser();
            this.chat = createTestChat(user1, user2);
            this.post = createTestPost(user1);
            this.comment = createTestComment(post, user2);
            this.message = createTestMessage(chat, user1, user2);
            this.like = createLikeReaction(user2, post.getId());
            this.dislike = createDislikeReaction(user1, post.getId());
            this.notification = createTestNotification(user1.getId(), user2.getId(), post.getId(), NotificationType.POST_REACTION);
        }
    }

    // Static method to get a complete test data set
    public static TestDataSet createCompleteDataSet() {
        return new TestDataSet();
    }
}
