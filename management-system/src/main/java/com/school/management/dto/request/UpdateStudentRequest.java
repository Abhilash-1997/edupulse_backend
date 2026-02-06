package com.school.management.dto.request;

import com.school.management.constant.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentRequest {
    private String name;
    private LocalDate dob;
    private Gender gender;

    private UUID parentId;
    private UUID classId;
    private UUID sectionId;
}
