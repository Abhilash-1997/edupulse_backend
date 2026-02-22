package com.school.management.dto.request;

import com.school.management.constant.DayOfWeek;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDailyTimetableRequest {

    @NotNull(message = "Section ID is required")
    private UUID sectionId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotEmpty(message = "Periods cannot be empty")
    @Valid
    private List<TimetablePeriod> periods;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimetablePeriod {

        @NotNull(message = "Subject ID is required")
        private UUID subjectId;

        @NotNull(message = "Teacher ID is required")
        private UUID teacherId;

        @NotBlank(message = "Start time is required")
        private String startTime;

        @NotBlank(message = "End time is required")
        private String endTime;

        private String classroom;
    }
}