package com.school.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSectionResponse {

    private UUID id;
    private String name;
    private UUID classId;
    private String className;
    private UUID classTeacherId;
    private String classTeacherName;
}