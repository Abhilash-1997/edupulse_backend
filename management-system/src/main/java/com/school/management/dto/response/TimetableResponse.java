package com.school.management.dto.response;

import com.school.management.constant.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableResponse {

    private UUID id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;
    private SubjectResponse subject;
    private UserResponse teacher;
    private ClassSectionResponse section;
}