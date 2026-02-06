package com.school.management.dto.response;

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
public class StudentResponse {

    private UUID id;
    private String name;
    private String admissionNumber;
    private LocalDate dob;
    private Gender gender;
    private String profilePicture;
    private UUID parentId;
    private UUID classId;
    private UUID sectionId;
    private ClassResponse classInfo;
    private ClassSectionResponse sectionInfo;
    private ParentResponse parent;
}