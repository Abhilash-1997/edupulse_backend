package com.school.management.dto.request;

import com.school.management.constant.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimetableRequest {

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotNull(message = "Section ID is required")
    private UUID sectionId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotNull(message = "Teacher ID is required")
    private UUID teacherId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private String day; // Fallback for dayOfWeek

    @NotBlank(message = "Start time is required")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Use HH:mm")
    private String startTime;

    @NotBlank(message = "End time is required")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Use HH:mm")
    private String endTime;

    private String room; // Maps to classroom
}