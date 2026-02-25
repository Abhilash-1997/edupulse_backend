package com.school.management.service;

import com.school.management.constant.ExamType;
import com.school.management.dto.request.AddExamResultRequest;
import com.school.management.dto.request.CreateExamRequest;
import com.school.management.dto.request.UpdateExamResultRequest;
import com.school.management.dto.response.ClassResponse;
import com.school.management.dto.response.ClassSectionResponse;
import com.school.management.dto.response.ExamResponse;
import com.school.management.dto.response.ExamResultResponse;
import com.school.management.dto.response.ExamResultsByStudentResponse;
import com.school.management.dto.response.ReportCardResponse;
import com.school.management.dto.response.StudentExamResultsResponse;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.spec.ExamResultSpecification;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final GradeRuleRepository gradeRuleRepository;
    private final ClassRepository classRepository;
    private final ClassSectionRepository classSectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final PdfGenerationService pdfGenerationService;

    // =========================== create exam

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                request.getClassId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        ClassSection section = null;
        if (request.getSectionId() != null) {
            section = classSectionRepository.findByIdAndDeletedAtIsNull(request.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        }

        Exam exam = Exam.builder()
                .school(school)
                .classEntity(classEntity)
                .section(section)
                .name(request.getName())
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();

        exam = examRepository.save(exam);

        return mapToExamResponse(exam);
    }

    // ============================ get all exams
    // ====================================

    @Transactional(readOnly = true)
    public List<ExamResponse> getAllExams(UUID classId, UUID sectionId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<Exam> exams;

        if (classId != null && sectionId != null) {
            exams = examRepository.findBySchoolAndClassAndSection(schoolId, classId, sectionId);
        } else if (classId != null) {
            exams = examRepository.findBySchoolAndClassOnly(schoolId, classId);
        } else {
            exams = examRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        }

        return exams.stream()
                .map(this::mapToExamResponse)
                .collect(Collectors.toList());
    }

    // ========================== add exam results
    // ==========================================

    @Transactional
    public List<ExamResultResponse> addExamResults(AddExamResultRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        Exam exam = examRepository.findByIdAndDeletedAtIsNull(request.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        Subject subject = subjectRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                request.getSubjectId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        List<ExamResultResponse> responses = new ArrayList<>();

        for (AddExamResultRequest.StudentResult studentResult : request.getResults()) {
            Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                    studentResult.getStudentId(), schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student not found: " + studentResult.getStudentId()));

            // Calculate grade
            Float maxMarks = studentResult.getMaxMarks() != null ? studentResult.getMaxMarks() : 100.0f;
            Float percentage = (studentResult.getMarksObtained() / maxMarks) * 100;
            String grade = calculateGrade(schoolId, percentage);

            // Find or create result
            ExamResult result = examResultRepository
                    .findByExam_IdAndStudent_IdAndSubject_IdAndDeletedAtIsNull(
                            exam.getId(), student.getId(), subject.getId())
                    .orElse(ExamResult.builder()
                            .school(exam.getSchool())
                            .exam(exam)
                            .student(student)
                            .subject(subject)
                            .build());

            result.setMarksObtained(studentResult.getMarksObtained());
            result.setMaxMarks(maxMarks);
            result.setGrade(grade != null ? grade : studentResult.getGrade());
            result.setRemarks(studentResult.getRemarks());

            result = examResultRepository.save(result);

            responses.add(mapToExamResultResponse(result, percentage));
        }

        return responses;
    }

    // ================================ update exam result
    // =============================================

    @Transactional
    public ExamResultResponse updateExamResult(UUID id, UpdateExamResultRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        ExamResult result = examResultRepository.findByIdAndSchool_IdAndDeletedAtIsNull(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found"));

        if (request.getMarksObtained() != null) {
            result.setMarksObtained(request.getMarksObtained());
        }
        if (request.getMaxMarks() != null) {
            result.setMaxMarks(request.getMaxMarks());
        }
        if (request.getGrade() != null) {
            result.setGrade(request.getGrade());
        }
        if (request.getRemarks() != null) {
            result.setRemarks(request.getRemarks());
        }

        // Recalculate grade if marks changed
        if (request.getMarksObtained() != null || request.getMaxMarks() != null) {
            Float percentage = (result.getMarksObtained() / result.getMaxMarks()) * 100;
            String grade = calculateGrade(schoolId, percentage);
            if (grade != null) {
                result.setGrade(grade);
            }
        }

        result = examResultRepository.save(result);

        Float percentage = (result.getMarksObtained() / result.getMaxMarks()) * 100;
        return mapToExamResultResponse(result, percentage);
    }

    // =========================== get exam results
    // ========================================

    @Transactional(readOnly = true)
    public List<ExamResultsByStudentResponse> getExamResults(UUID examId, UUID subjectId,
            UUID classId, UUID sectionId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<ExamResult> results = examResultRepository.findAll(
                ExamResultSpecification.withFilters(schoolId, examId, subjectId, classId, sectionId));

        // Group results by student
        Map<UUID, List<ExamResult>> resultsByStudent = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStudent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ExamResultsByStudentResponse> responses = new ArrayList<>();

        for (Map.Entry<UUID, List<ExamResult>> entry : resultsByStudent.entrySet()) {
            List<ExamResult> studentResults = entry.getValue();
            Student student = studentResults.get(0).getStudent();

            float totalObtained = 0;
            float totalMax = 0;
            boolean isFailed = false;

            List<ExamResultsByStudentResponse.SubjectResult> subjectResults = new ArrayList<>();

            for (ExamResult r : studentResults) {
                float pct = r.getMaxMarks() > 0 ? (r.getMarksObtained() / r.getMaxMarks()) * 100 : 0;
                String grade = r.getGrade();
                if (grade == null) {
                    grade = calculateGrade(schoolId, pct);
                }
                if ("F".equalsIgnoreCase(grade)) {
                    isFailed = true;
                }

                subjectResults.add(ExamResultsByStudentResponse.SubjectResult.builder()
                        .subject(ExamResultsByStudentResponse.SubjectInfo.builder()
                                .id(r.getSubject().getId())
                                .name(r.getSubject().getName())
                                .code(r.getSubject().getCode())
                                .build())
                        .marksObtained(r.getMarksObtained())
                        .maxMarks(r.getMaxMarks())
                        .grade(grade)
                        .build());

                totalObtained += r.getMarksObtained();
                totalMax += r.getMaxMarks();
            }

            float percentage = totalMax > 0 ? Math.round((totalObtained / totalMax) * 1000) / 10.0f : 0;

            responses.add(ExamResultsByStudentResponse.builder()
                    .student(ExamResultsByStudentResponse.StudentInfo.builder()
                            .id(student.getId())
                            .name(student.getName())
                            .admissionNumber(student.getAdmissionNumber())
                            .build())
                    .totalObtained(totalObtained)
                    .totalMax(totalMax)
                    .percentage(percentage)
                    .isFailed(isFailed)
                    .results(subjectResults)
                    .build());
        }

        return responses;
    }

    // =========================== get student report card
    // ========================================

    @Transactional(readOnly = true)
    public List<ReportCardResponse> getStudentReportCard(UUID studentId, UUID examId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<ExamResult> results;
        if (examId != null) {
            results = examResultRepository.findByStudent_IdAndExam_IdAndSchool_IdAndDeletedAtIsNull(
                    studentId, examId, schoolId);
        } else {
            results = examResultRepository.findByStudent_IdAndSchool_IdAndDeletedAtIsNull(
                    studentId, schoolId);
        }

        return results.stream()
                .map(this::mapToReportCardResponse)
                .collect(Collectors.toList());
    }

    // =========================== get student exam results
    // ========================================

    @Transactional(readOnly = true)
    public StudentExamResultsResponse getStudentExamResults(UUID studentId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        if (studentId == null) {
            throw new BadRequestException("Student ID is required");
        }

        // Verify student exists and belongs to this school
        Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Fetch all exam results for the student
        List<ExamResult> results = examResultRepository.findByStudent_IdAndSchool_IdAndDeletedAtIsNull(
                studentId, schoolId);

        // Build class name
        String className = buildClassName(student);

        // Group results by exam
        Map<UUID, StudentExamResultsResponse.ExamGroup.ExamGroupBuilder> examGroupBuilders = new LinkedHashMap<>();
        Map<UUID, List<StudentExamResultsResponse.SubjectResult>> examSubjects = new LinkedHashMap<>();
        Map<UUID, Float> examTotalObtained = new HashMap<>();
        Map<UUID, Float> examTotalMax = new HashMap<>();

        // Sort results by exam start date DESC
        results.sort((a, b) -> {
            if (a.getExam().getStartDate() == null || b.getExam().getStartDate() == null)
                return 0;
            return b.getExam().getStartDate().compareTo(a.getExam().getStartDate());
        });

        for (ExamResult result : results) {
            UUID eId = result.getExam().getId();

            if (!examGroupBuilders.containsKey(eId)) {
                examGroupBuilders.put(eId, StudentExamResultsResponse.ExamGroup.builder()
                        .examId(result.getExam().getId())
                        .examName(result.getExam().getName())
                        .examType(result.getExam().getType())
                        .startDate(result.getExam().getStartDate())
                        .endDate(result.getExam().getEndDate())
                        .className(className)
                        .classId(student.getClassEntity() != null ? student.getClassEntity().getId() : null));
                examSubjects.put(eId, new ArrayList<>());
                examTotalObtained.put(eId, 0.0f);
                examTotalMax.put(eId, 0.0f);
            }

            Float pct = result.getMaxMarks() > 0
                    ? (result.getMarksObtained() / result.getMaxMarks()) * 100
                    : 0.0f;

            examSubjects.get(eId).add(StudentExamResultsResponse.SubjectResult.builder()
                    .subjectId(result.getSubject().getId())
                    .subjectName(result.getSubject().getName())
                    .subjectCode(result.getSubject().getCode())
                    .marksObtained(result.getMarksObtained())
                    .maxMarks(result.getMaxMarks())
                    .percentage(String.format("%.1f", pct))
                    .grade(result.getGrade())
                    .remarks(result.getRemarks())
                    .build());

            examTotalObtained.merge(eId, result.getMarksObtained(), Float::sum);
            examTotalMax.merge(eId, result.getMaxMarks(), Float::sum);
        }

        // Build exam groups with overall percentage
        List<StudentExamResultsResponse.ExamGroup> examGroups = new ArrayList<>();
        for (UUID eId : examGroupBuilders.keySet()) {
            Float totalObtained = examTotalObtained.get(eId);
            Float totalMax = examTotalMax.get(eId);
            String overallPercentage = totalMax > 0
                    ? String.format("%.2f", (totalObtained / totalMax) * 100)
                    : "0";

            examGroups.add(examGroupBuilders.get(eId)
                    .subjects(examSubjects.get(eId))
                    .totalObtained(totalObtained)
                    .totalMax(totalMax)
                    .overallPercentage(overallPercentage)
                    .build());
        }

        return StudentExamResultsResponse.builder()
                .student(StudentExamResultsResponse.StudentInfo.builder()
                        .id(student.getId())
                        .name(student.getName())
                        .admissionNumber(student.getAdmissionNumber())
                        .className(className)
                        .build())
                .examResults(examGroups)
                .build();
    }

    // =========================== download report card (PDF)
    // ========================================

    @Transactional(readOnly = true)
    public byte[] downloadReportCard(UUID studentId, UUID examId) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        if (studentId == null || examId == null) {
            throw new BadRequestException("Student ID and Exam ID are required");
        }

        // Fetch student with all related info
        Student student = studentRepository.findByIdAndSchoolIdWithDetails(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Fetch exam
        Exam exam = examRepository.findByIdAndDeletedAtIsNull(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        // Fetch results for this student and exam
        List<ExamResult> results = examResultRepository.findByStudent_IdAndExam_IdAndSchool_IdAndDeletedAtIsNull(
                studentId, examId, schoolId);

        // Fetch all exam results for ranking
        List<ExamResult> allExamResults = examResultRepository.findByExam_IdAndSchool_IdAndDeletedAtIsNull(
                examId, schoolId);

        // Calculate student scores for ranking
        Map<UUID, Float> studentScores = new HashMap<>();
        for (ExamResult r : allExamResults) {
            studentScores.merge(r.getStudent().getId(), r.getMarksObtained(), Float::sum);
        }

        // Sort scores descending for rank
        List<Map.Entry<UUID, Float>> sortedScores = studentScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                .collect(Collectors.toList());

        int rank = 0;
        for (int i = 0; i < sortedScores.size(); i++) {
            if (sortedScores.get(i).getKey().equals(studentId)) {
                rank = i + 1;
                break;
            }
        }
        int totalStudents = sortedScores.size();

        // Fetch grade rules for this school
        List<GradeRule> gradeRules = gradeRuleRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

        // Build subject details with grades
        boolean[] hasFailed = { false };
        List<Map<String, Object>> subjectDetails = new ArrayList<>();

        float totalMarksObtained = 0;
        float totalMaxMarks = 0;

        for (ExamResult r : results) {
            float p = (r.getMarksObtained() / r.getMaxMarks()) * 100;
            String grade = getGradeFromRules(gradeRules, p);
            if ("F".equals(grade)) {
                hasFailed[0] = true;
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("subjectName", r.getSubject().getName());
            detail.put("subjectCode", r.getSubject().getCode());
            detail.put("marksObtained", r.getMarksObtained());
            detail.put("maxMarks", r.getMaxMarks());
            detail.put("percentage", String.format("%.1f", p));
            detail.put("grade", grade);
            detail.put("remarks", r.getRemarks());
            subjectDetails.add(detail);

            totalMarksObtained += r.getMarksObtained();
            totalMaxMarks += r.getMaxMarks();
        }

        String overallPercentage = totalMaxMarks > 0
                ? String.format("%.2f", (totalMarksObtained / totalMaxMarks) * 100)
                : "0";

        String overallGrade = getGradeFromRules(gradeRules, Float.parseFloat(overallPercentage));
        if (hasFailed[0]) {
            overallGrade = "F";
        }

        // Build class name
        String className = buildClassName(student);

        // Prepare template variables
        Map<String, Object> variables = new HashMap<>();
        // School info
        School school = student.getSchool();
        variables.put("schoolName", school != null ? school.getName() : "");
        variables.put("schoolAddress", school != null ? school.getAddress() : "");
        variables.put("schoolLogo", school != null ? school.getLogo() : null);
        variables.put("schoolBoard", school != null ? school.getBoard() : "");
        variables.put("academicYear", school != null ? school.getAcademicYear() : "");

        // Student info
        variables.put("studentName", student.getName());
        variables.put("admissionNumber", student.getAdmissionNumber());
        variables.put("className", className);
        variables.put("studentDob", student.getDob() != null ? student.getDob().toString() : "");
        variables.put("studentGender", student.getGender() != null ? student.getGender().name() : "");

        // Parent/Guardian info
        if (student.getParent() != null) {
            variables.put("guardianName", student.getParent().getGuardianName());
            variables.put("guardianPhone",
                    student.getParent().getUser() != null ? student.getParent().getUser().getPhone() : "");
        } else {
            variables.put("guardianName", "N/A");
            variables.put("guardianPhone", "");
        }

        // Exam info
        variables.put("examName", exam.getName());
        variables.put("examType", exam.getType() != null ? exam.getType().name() : "");

        // Results
        variables.put("subjects", subjectDetails);
        variables.put("totalMarksObtained", totalMarksObtained);
        variables.put("totalMaxMarks", totalMaxMarks);
        variables.put("overallPercentage", overallPercentage);
        variables.put("overallGrade", overallGrade);
        variables.put("rank", rank);
        variables.put("totalStudents", totalStudents);

        return pdfGenerationService.generatePdf("report-card", variables);
    }

    // ================================ Private Helper Methods
    // ==========================================

    private String calculateGrade(UUID schoolId, Float percentage) {
        return gradeRuleRepository.findBySchoolIdAndPercentage(schoolId, percentage)
                .map(GradeRule::getGrade)
                .orElse(null);
    }

    private String getGradeFromRules(List<GradeRule> gradeRules, float percentage) {
        for (GradeRule rule : gradeRules) {
            if (percentage >= rule.getMinPercentage() && percentage <= rule.getMaxPercentage()) {
                return rule.getGrade();
            }
        }
        return "-";
    }

    private String buildClassName(Student student) {
        if (student.getClassEntity() != null && student.getSection() != null) {
            return student.getClassEntity().getName() + " - " + student.getSection().getName();
        } else if (student.getClassEntity() != null) {
            return student.getClassEntity().getName();
        }
        return "N/A";
    }

    private ExamResponse mapToExamResponse(Exam exam) {
        ClassResponse classInfo = null;
        if (exam.getClassEntity() != null) {
            classInfo = ClassResponse.builder()
                    .id(exam.getClassEntity().getId())
                    .name(exam.getClassEntity().getName())
                    .build();
        }

        ClassSectionResponse sectionInfo = null;
        if (exam.getSection() != null) {
            sectionInfo = ClassSectionResponse.builder()
                    .id(exam.getSection().getId())
                    .name(exam.getSection().getName())
                    .classId(exam.getClassEntity() != null ? exam.getClassEntity().getId() : null)
                    .className(exam.getClassEntity() != null ? exam.getClassEntity().getName() : null)
                    .build();
        }

        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .type(exam.getType())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .isActive(exam.getIsActive())
                .classInfo(classInfo)
                .section(sectionInfo)
                .build();
    }

    private ExamResultResponse mapToExamResultResponse(ExamResult result, Float percentage) {
        return ExamResultResponse.builder()
                .id(result.getId())
                .marksObtained(result.getMarksObtained())
                .maxMarks(result.getMaxMarks())
                .grade(result.getGrade())
                .remarks(result.getRemarks())
                .percentage(percentage)
                .examId(result.getExam().getId())
                .studentId(result.getStudent().getId())
                .subjectId(result.getSubject().getId())
                .subjectName(result.getSubject().getName())
                .build();
    }

    private ReportCardResponse mapToReportCardResponse(ExamResult result) {
        return ReportCardResponse.builder()
                .id(result.getId())
                .examId(result.getExam().getId())
                .studentId(result.getStudent().getId())
                .subjectId(result.getSubject().getId())
                .marksObtained(result.getMarksObtained())
                .maxMarks(result.getMaxMarks())
                .grade(result.getGrade())
                .remarks(result.getRemarks())
                .subjectName(result.getSubject().getName())
                .subjectCode(result.getSubject().getCode())
                .examName(result.getExam().getName())
                .examType(result.getExam().getType())
                .build();
    }
}