package com.school.management.controller;

import com.school.management.dto.request.SendMessageRequest;
import com.school.management.dto.response.*;
import com.school.management.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','STUDENT','SCHOOL_ADMIN')")
public class ChatController {

    private final ChatService chatService;

    // ================================
    // Conversations
    // ================================

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations() {
        List<ConversationResponse> response = chatService.getConversations();
        return ResponseEntity.ok(
                ApiResponse.success("Conversations fetched successfully", response)
        );
    }

    @PostMapping("/conversations/{otherUserId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @PathVariable UUID otherUserId) {

        ConversationResponse response =
                chatService.getOrCreateConversation(otherUserId);

        return ResponseEntity.ok(
                ApiResponse.success("Conversation fetched successfully", response)
        );
    }

    // ================================
    // Messages
    // ================================

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<ApiResponse<MessagesResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursor) {

        MessagesResponse response =
                chatService.getMessages(conversationId, limit, cursor);

        return ResponseEntity.ok(
                ApiResponse.success("Messages fetched successfully", response)
        );
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {

        MessageResponse response = chatService.sendMessage(request);

        return ResponseEntity.ok(
                ApiResponse.success("Message sent successfully", response)
        );
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markRead(
            @PathVariable UUID messageId) {

        MessageResponse response = chatService.markRead(messageId);

        return ResponseEntity.ok(
                ApiResponse.success("Message marked as read", response)
        );
    }

    // ================================
    // Chat Users
    // ================================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getChatUsers(
            @RequestParam(required = false) String search) {

        List<UserResponse> response = chatService.getChatUsers(search);

        return ResponseEntity.ok(
                ApiResponse.success("Chat users fetched successfully", response)
        );
    }
}
