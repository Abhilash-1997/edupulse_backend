package com.school.management.dto.response;

import com.school.management.constant.NotificationStatus;
import com.school.management.constant.NotificationType;
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
public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private NotificationStatus status;
    private NotificationType type;
    private LocalDateTime createdAt;
}