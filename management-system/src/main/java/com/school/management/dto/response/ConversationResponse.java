package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;
    private UserResponse otherUser;
    private MessageResponse lastMessage;
    private Integer unreadCount;
    private LocalDateTime updatedAt;
}