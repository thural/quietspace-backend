package dev.thural.quietspace.chat.controller;

import dev.thural.quietspace.chat.ChatService;
import dev.thural.quietspace.chat.dto.ChatResponse;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.chat.dto.UpdateChatRequest;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public static final String CHAT_PATH = "/api/v1/chats";
    public static final String CHAT_REMOVE_MEMBER_PATH = "/{chatId}/members/remove/{userId}";

    @GetMapping
    ResponseEntity<List<ChatResponse>> getMyChats() {
        UUID userId = userService.getSignedUser().getId();
        return ResponseEntity.ok(chatService.getChatsByUserId(userId));
    }

    @GetMapping("/{chatId}")
    ResponseEntity<ChatResponse> getSingleChatById(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    @GetMapping("/members/{userId}")
    ResponseEntity<List<ChatResponse>> getChatsByMemberId(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatService.getChatsByUserId(userId));
    }

    @PostMapping
    ResponseEntity<ChatResponse> createChat(@RequestBody CreateChatRequest chat) {
        return ResponseEntity.ok(chatService.createChat(chat));
    }

    @PatchMapping("/{chatId}")
    ResponseEntity<ChatResponse> updateChat(@PathVariable UUID chatId, @RequestBody UpdateChatRequest request) {
        return ResponseEntity.ok(chatService.updateChat(chatId, request));
    }

    @PatchMapping("/{chatId}/members/add/{userId}")
    ResponseEntity<UserResponse> addMemberWithId(@PathVariable UUID userId, @PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.addMemberWithId(userId, chatId));
    }

    @PatchMapping(CHAT_REMOVE_MEMBER_PATH)
    ResponseEntity<?> removeMemberWithId(@PathVariable UUID chatId, @PathVariable UUID userId) {
        chatService.removeMemberWithId(userId, chatId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{chatId}")
    ResponseEntity<?> deleteChatWithId(@PathVariable UUID chatId) {
        chatService.deleteChatById(chatId);
        return ResponseEntity.noContent().build();
    }

}
