package dev.thural.quietspace.message.controller;

import dev.thural.quietspace.message.MessageService;
import dev.thural.quietspace.message.dto.MessageRequest;
import dev.thural.quietspace.message.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    public static final String MESSAGE_PATH = "/api/v1/messages";
    public static final String MESSAGE_PATH_ID = "/{messageId}";

    private final MessageService messageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<MessageResponse> createMessage(
            @RequestPart @Valid MessageRequest messageRequest,
            @RequestPart(value = "photoData", required = false) MultipartFile photoData) {
        messageRequest.setPhotoData(photoData);
        return ResponseEntity.ok(messageService.addMessage(messageRequest));
    }

    @DeleteMapping(MESSAGE_PATH_ID)
    ResponseEntity<?> deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chat/{chatId}")
    Page<MessageResponse> getMessagesByChatId(
            @RequestParam(name = "page-number", required = false) Integer pageNumber,
            @RequestParam(name = "page-size", required = false) Integer pageSize,
            @PathVariable UUID chatId
    ) {
        return messageService.getMessagesByChatId(pageNumber, pageSize, chatId);
    }

    @GetMapping("/chat/{chatId}/message/{messageId}")
    ResponseEntity<MessageResponse> getMessageById(@PathVariable UUID messageId, @PathVariable UUID chatId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId, chatId));
    }

    @GetMapping("/unread")
    ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(messageService.getUnreadCount());
    }

    @PutMapping("/{messageId}/read")
    ResponseEntity<MessageResponse> markAsRead(@PathVariable UUID messageId) {
        return messageService.setMessageSeen(messageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
