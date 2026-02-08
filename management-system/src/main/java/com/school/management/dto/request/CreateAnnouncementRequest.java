package com.school.management.dto.request;

import com.school.management.constant.AnnouncementPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnnouncementRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private AnnouncementPriority priority;

    private LocalDateTime expiryDate;

    // For SUPER_ADMIN: bulk create for multiple schools
    private List<UUID> targetSchoolIds;

    // For TEACHER: target specific class
    private UUID targetClassId;
}