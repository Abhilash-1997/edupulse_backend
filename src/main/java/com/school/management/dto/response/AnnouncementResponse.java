package com.school.management.dto.response;

import com.school.management.constant.AnnouncementPriority;
import com.school.management.constant.UserRole;
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
public class AnnouncementResponse {

    private UUID id;
    private String title;
    private String message;
    private AnnouncementPriority priority;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private UserRole authorRole;
    private UUID authorId;
    private String authorName;
    private UUID schoolId;
    private UUID classId;
    private String className;
    private LocalDateTime createdAt;
}