package dev.thural.quietspace.domain.post.controller;

import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.post.PostService;
import dev.thural.quietspace.domain.post.dto.PostRequest;
import dev.thural.quietspace.domain.post.dto.PostResponse;
import dev.thural.quietspace.domain.post.dto.RepostRequest;
import dev.thural.quietspace.domain.post.dto.VoteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static dev.thural.quietspace.domain.notification.NotificationType.REPOST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    public static final String POST_PATH = "/api/v1/posts";
    public static final String POST_PATH_ID = "/{postId}";

    private final PostService postService;
    private final NotificationService notificationService;

    @GetMapping
    Page<PostResponse> getAllPosts(
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        log.info("pageNumber {} pageSize {}", pageNumber, pageSize);
        return postService.getAllPosts(pageNumber, pageSize);
    }

    @GetMapping("/search")
    Page<PostResponse> getPostsByQuery(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return postService.getAllByQuery(query, pageNumber, pageSize);
    }

    @GetMapping("/user/{userId}")
    public Page<PostResponse> listUserPosts(
            @PathVariable UUID userId,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return postService.getPostsByUserId(userId, pageNumber, pageSize);
    }

    @GetMapping("/user/{userId}/commented")
    public Page<PostResponse> listCommentedPosts(
            @PathVariable UUID userId,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return postService.getCommentedPostsByUserId(userId, pageNumber, pageSize);
    }

    @GetMapping("/saved")
    Page<PostResponse> getSavedPostsByUserId(
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return postService.getSavedPostsByUser(pageNumber, pageSize);
    }

    @PostMapping("/{postId}/save")
    ResponseEntity<Void> savePostForUser(@PathVariable UUID postId) {
        postService.savePostForUser(postId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/save")
    ResponseEntity<Void> unsavePostForUser(@PathVariable UUID postId) {
        postService.unsavePostForUser(postId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/saved/{postId}")
    ResponseEntity<Void> savePost(@PathVariable UUID postId) {
        postService.savePostForUser(postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<PostResponse> createPost(
            @RequestPart @Valid PostRequest post,
            @RequestPart(value = "photoData", required = false) MultipartFile photoData) {
        post.setPhotoData(photoData);
        return ResponseEntity.ok(postService.addPost(post));
    }

    @PostMapping("/repost")
    ResponseEntity<PostResponse> createRepost(@RequestBody @Validated RepostRequest repost) {
        PostResponse response = postService.addRepost(repost);
        notificationService.processNotification(REPOST, repost.getPostId());
        return ResponseEntity.ok(response);
    }

    @GetMapping(POST_PATH_ID)
    ResponseEntity<PostResponse> getPostById(@PathVariable UUID postId) {
        return postService.getPostById(postId)
                .map(post -> ResponseEntity.ok().body(post))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(POST_PATH_ID)
    @PreAuthorize("@postSecurity.canAccess(#postId, authentication.name)")
    ResponseEntity<PostResponse> putPost(@PathVariable UUID postId, @RequestBody @Validated PostRequest post) {
        return ResponseEntity.ok(postService.updatePost(postId, post));
    }

    @DeleteMapping(POST_PATH_ID)
    @PreAuthorize("@postSecurity.canAccess(#postId, authentication.name)")
    ResponseEntity<?> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(POST_PATH_ID)
    @PreAuthorize("@postSecurity.canAccess(#postId, authentication.name)")
    ResponseEntity<PostResponse> patchPost(@PathVariable UUID postId, @RequestBody PostRequest post) {
        return ResponseEntity.ok(postService.patchPost(postId, post));
    }

    @PostMapping("/vote-poll")
    ResponseEntity<?> votePoll(@RequestBody VoteRequest voteRequest) {
        postService.votePoll(voteRequest);
        return ResponseEntity.ok().build();
    }

}
