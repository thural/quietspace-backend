package dev.thural.quietspace.domain.reaction.controller;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reactions")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;
    private final NotificationService notificationService;

    @GetMapping("/user")
    Page<ReactionResponse> getReactionsByUser(
            @RequestParam UUID userId,
            @RequestParam EntityType contentType,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return reactionService.getReactionsByUserIdAndContentType(userId, contentType, pageNumber, pageSize);
    }

    @GetMapping("/content")
    Page<ReactionResponse> getReactionsByContent(
            @RequestParam UUID contentId,
            @RequestParam EntityType contentType,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return reactionService.getReactionsByContentIdAndContentType(contentId, contentType, pageNumber, pageSize);
    }

    @PostMapping
    ResponseEntity<Void> addReaction(@RequestBody ReactionRequest reaction) {
        reactionService.addReaction(reaction);
        notificationService.processNotificationByReaction(reaction.getContentType(), reaction.getContentId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reactionId}")
    ResponseEntity<Void> removeReaction(@PathVariable UUID reactionId) {
        reactionService.removeReaction(reactionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/toggle-reaction")
    ResponseEntity<?> toggleReaction(@RequestBody ReactionRequest reaction) {
        reactionService.handleReaction(reaction);
        notificationService.processNotificationByReaction(reaction.getContentType(), reaction.getContentId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/post/{postId}")
    Page<ReactionResponse> getReactionsByPostId(
            @PathVariable UUID postId,
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize
    ) {
        return reactionService.getReactionsByContentIdAndContentType(postId, EntityType.POST, pageNumber, pageSize);
    }

    @GetMapping("/count")
    ResponseEntity<Integer> countByContentIdAndReactionType(@RequestParam UUID contentId, @RequestParam ReactionType type) {
        return ResponseEntity.ok(reactionService.countByContentIdAndReactionType(contentId, type));
    }

}
