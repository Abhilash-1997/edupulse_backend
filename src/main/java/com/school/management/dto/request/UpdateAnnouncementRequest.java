package com.school.management.dto.request;

import com.school.management.constant.AnnouncementPriority;
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
public class UpdateAnnouncementRequest {

    private String title;
    private String message;
    private AnnouncementPriority priority;
    private LocalDateTime expiryDate;

    // For SUPER_ADMIN updates
    private List<UUID> targetSchoolIds;
}