package com.school.management.dto.response;

import com.school.management.constant.MessageStatus;
import com.school.management.constant.MessageType;
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
public class MessageResponse {

    private UUID id;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private UUID senderId;
    private UUID receiverId;
    private UUID conversationId;
    private UserResponse sender;
    private LocalDateTime createdAt;
}