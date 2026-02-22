package com.school.management.dto.response;

import com.school.management.constant.ComplaintPriority;
import com.school.management.constant.ComplaintStatus;
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
public class ComplaintResponse {

    private UUID id;
    private String title;
    private String description;
    private ComplaintStatus status;
    private ComplaintPriority priority;
    private UUID parentId;
    private String guardianName;
    private String occupation;
    private LocalDateTime createdAt;
}