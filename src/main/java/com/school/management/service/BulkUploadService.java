package com.school.management.service;

import com.school.management.constant.AttendanceStatus;
import com.school.management.constant.ExamType;
import com.school.management.constant.Gender;
import com.school.management.constant.UserRole;
import com.school.management.dto.response.BulkUploadResponse;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final ClassSectionRepository classSectionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final SubjectRepository subjectRepository;
    private final LibrarySectionRepository librarySectionRepository;
    private final BookRepository bookRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================= STUDENT + PARENT UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadStudentParent(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String studentName = getRequired(row, "StudentName", rowNum);
                String admissionNumber = getRequired(row, "AdmissionNumber", rowNum);
                String parentEmail = getRequired(row, "ParentEmail", rowNum);
                String parentName = getRequired(row, "ParentName", rowNum);

                // 1. Process Parent (User + Parent profile)
                Optional<User> existingUser = userRepository.findByEmailAndDeletedAtIsNull(parentEmail);
                Parent parentProfile;

                if (existingUser.isPresent()) {
                    User parentUser = existingUser.get();
                    if (parentUser.getRole() != UserRole.PARENT) {
                        throw new RuntimeException("Email " + parentEmail + " belongs to a non-parent user");
                    }
                    parentProfile = parentRepository.findByUser_IdAndDeletedAtIsNull(parentUser.getId())
                            .orElseThrow(() -> new RuntimeException("Parent profile not found for " + parentEmail));
                } else {
                    // Create new parent user
                    User parentUser = User.builder()
                            .name(parentName)
                            .email(parentEmail)
                            .phone(getOptional(row, "ParentPhone"))
                            .passwordHash(passwordEncoder.encode("password123"))
                            .role(UserRole.PARENT)
                            .school(school)
                            .build();
                    parentUser = userRepository.save(parentUser);

                    parentProfile = Parent.builder()
                            .user(parentUser)
                            .school(school)
                            .guardianName(parentName)
                            .occupation(getOptional(row, "Occupation"))
                            .build();
                    parentProfile = parentRepository.save(parentProfile);
                }

                // 2. Check duplicate student
                if (studentRepository.existsBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(schoolId,
                        admissionNumber)) {
                    throw new RuntimeException("Student with Admission Number " + admissionNumber + " already exists");
                }

                // 3. Resolve class
                ClassEntity classEntity = null;
                String className = getOptional(row, "ClassName");
                if (className != null) {
                    classEntity = classRepository.findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, className)
                            .orElse(null);
                }

                // 4. Resolve section by SectionName + ClassName
                ClassSection section = null;
                String sectionName = getOptional(row, "SectionName");
                if (sectionName != null && className != null) {
                    section = classSectionRepository.findByNameAndClassNameAndSchoolId(sectionName, className, schoolId)
                            .orElse(null);
                }

                // 5. Parse DOB and Gender
                LocalDate dob = parseDate(getOptional(row, "DOB"));
                if (dob == null)
                    dob = LocalDate.now();

                Gender gender;
                try {
                    gender = Gender.valueOf(getOptional(row, "Gender") != null ? getOptional(row, "Gender") : "Other");
                } catch (IllegalArgumentException e) {
                    gender = Gender.Other;
                }

                // 6. Create Student
                Student student = Student.builder()
                        .school(school)
                        .parent(parentProfile)
                        .name(studentName)
                        .admissionNumber(admissionNumber)
                        .dob(dob)
                        .gender(gender)
                        .classEntity(classEntity)
                        .section(section)
                        .build();
                studentRepository.save(student);

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= ATTENDANCE UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadAttendance(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String admissionNumber = getRequired(row, "AdmissionNumber", rowNum);
                String dateStr = getRequired(row, "Date", rowNum);
                String statusStr = getRequired(row, "Status", rowNum);

                Student student = studentRepository
                        .findBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(schoolId, admissionNumber)
                        .orElseThrow(() -> new RuntimeException("Student " + admissionNumber + " not found"));

                LocalDate date = parseDate(dateStr);
                if (date == null)
                    throw new RuntimeException("Invalid date format: " + dateStr);

                AttendanceStatus status;
                try {
                    status = AttendanceStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(
                            "Invalid status: " + statusStr + ". Must be PRESENT, ABSENT, LATE, or HALF_DAY");
                }

                // Find or create attendance
                Optional<Attendance> existingAttendance = attendanceRepository
                        .findBySchool_IdAndStudent_IdAndDateAndDeletedAtIsNull(schoolId, student.getId(), date);

                if (existingAttendance.isPresent()) {
                    Attendance attendance = existingAttendance.get();
                    attendance.setStatus(status);
                    attendanceRepository.save(attendance);
                } else {
                    // Attendance requires a section - use student's section
                    ClassSection section = student.getSection();
                    if (section == null) {
                        throw new RuntimeException("Student " + admissionNumber + " has no section assigned");
                    }

                    Attendance attendance = Attendance.builder()
                            .school(school)
                            .student(student)
                            .section(section)
                            .date(date)
                            .status(status)
                            .build();
                    attendanceRepository.save(attendance);
                }

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= EXAM UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadExams(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String name = getRequired(row, "Name", rowNum);
                String typeStr = getRequired(row, "Type", rowNum);
                String startDateStr = getRequired(row, "StartDate", rowNum);
                String className = getRequired(row, "ClassName", rowNum);

                // Resolve class
                ClassEntity classEntity = classRepository.findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, className)
                        .orElseThrow(() -> new RuntimeException("Class " + className + " not found"));

                // Resolve section (optional)
                ClassSection section = null;
                String sectionName = getOptional(row, "SectionName");
                if (sectionName != null) {
                    section = classSectionRepository.findByNameAndClassNameAndSchoolId(sectionName, className, schoolId)
                            .orElse(null);
                }

                ExamType examType;
                try {
                    examType = ExamType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(
                            "Invalid exam type: " + typeStr + ". Must be UNIT_TEST, HALF_YEARLY, FINAL, or OTHER");
                }

                LocalDate startDate = parseDate(startDateStr);
                if (startDate == null)
                    throw new RuntimeException("Invalid start date: " + startDateStr);

                String endDateStr = getOptional(row, "EndDate");
                LocalDate endDate = endDateStr != null ? parseDate(endDateStr) : startDate;
                if (endDate == null)
                    endDate = startDate;

                Exam exam = Exam.builder()
                        .school(school)
                        .classEntity(classEntity)
                        .section(section)
                        .name(name)
                        .type(examType)
                        .startDate(startDate)
                        .endDate(endDate)
                        .build();
                examRepository.save(exam);

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= EXAM RESULTS UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadExamResults(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String examName = getRequired(row, "ExamName", rowNum);
                String admissionNumber = getRequired(row, "AdmissionNumber", rowNum);
                String subjectCode = getRequired(row, "SubjectCode", rowNum);
                String marksStr = getRequired(row, "Marks", rowNum);
                String totalMarksStr = getRequired(row, "TotalMarks", rowNum);

                // Find Exam
                Exam exam = examRepository.findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, examName)
                        .orElseThrow(() -> new RuntimeException("Exam " + examName + " not found"));

                // Find Student
                Student student = studentRepository
                        .findBySchool_IdAndAdmissionNumberAndDeletedAtIsNull(schoolId, admissionNumber)
                        .orElseThrow(() -> new RuntimeException("Student " + admissionNumber + " not found"));

                // Find Subject
                Subject subject = subjectRepository.findBySchool_IdAndCodeAndDeletedAtIsNull(schoolId, subjectCode)
                        .orElseThrow(() -> new RuntimeException("Subject " + subjectCode + " not found"));

                float marks = Float.parseFloat(marksStr);
                float totalMarks = Float.parseFloat(totalMarksStr);

                // Create or update
                Optional<ExamResult> existingResult = examResultRepository
                        .findByExam_IdAndStudent_IdAndSubject_IdAndDeletedAtIsNull(exam.getId(), student.getId(),
                                subject.getId());

                if (existingResult.isPresent()) {
                    ExamResult result = existingResult.get();
                    result.setMarksObtained(marks);
                    result.setMaxMarks(totalMarks);
                    String grade = getOptional(row, "Grade");
                    if (grade != null)
                        result.setGrade(grade);
                    String remarks = getOptional(row, "Remarks");
                    if (remarks != null)
                        result.setRemarks(remarks);
                    examResultRepository.save(result);
                } else {
                    ExamResult result = ExamResult.builder()
                            .school(school)
                            .exam(exam)
                            .student(student)
                            .subject(subject)
                            .marksObtained(marks)
                            .maxMarks(totalMarks)
                            .grade(getOptional(row, "Grade") != null ? getOptional(row, "Grade") : "")
                            .remarks(getOptional(row, "Remarks") != null ? getOptional(row, "Remarks") : "")
                            .build();
                    examResultRepository.save(result);
                }

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= LIBRARY SECTIONS UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadLibrarySections(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String name = getRequired(row, "Name", rowNum);

                // Check duplicate
                if (librarySectionRepository.findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, name).isPresent()) {
                    throw new RuntimeException("Section " + name + " already exists");
                }

                LibrarySection section = LibrarySection.builder()
                        .school(school)
                        .name(name)
                        .description(getOptional(row, "Description"))
                        .location(getOptional(row, "Location"))
                        .build();
                librarySectionRepository.save(section);

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= BOOKS UPLOAD =========================

    @Transactional
    public BulkUploadResponse uploadBooks(MultipartFile file) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();
        List<Map<String, String>> rows = parseExcel(file);
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;

            try {
                String title = getRequired(row, "Title", rowNum);
                String author = getRequired(row, "Author", rowNum);
                String isbn = getRequired(row, "ISBN", rowNum);
                String category = getRequired(row, "Category", rowNum);
                String quantityStr = getRequired(row, "Quantity", rowNum);
                String sectionName = getRequired(row, "SectionName", rowNum);

                // Find library section
                LibrarySection section = librarySectionRepository
                        .findBySchool_IdAndNameAndDeletedAtIsNull(schoolId, sectionName)
                        .orElseThrow(() -> new RuntimeException("Section " + sectionName + " not found"));

                // Check duplicate ISBN
                if (bookRepository.findBySchool_IdAndIsbnAndDeletedAtIsNull(schoolId, isbn).isPresent()) {
                    throw new RuntimeException("Book with ISBN " + isbn + " already exists");
                }

                int quantity = Integer.parseInt(quantityStr);

                Book book = Book.builder()
                        .school(school)
                        .section(section)
                        .title(title)
                        .author(author)
                        .isbn(isbn)
                        .publisher(getOptional(row, "Publisher"))
                        .category(category)
                        .quantity(quantity)
                        .available(quantity)
                        .description(getOptional(row, "Description"))
                        .build();
                bookRepository.save(book);

                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + ": " + e.getMessage());
            }
        }

        return buildResponse(rows.size(), successCount, errors);
    }

    // ========================= HELPER METHODS =========================

    /**
     * Parse an Excel file (.xlsx) into a list of row maps (header → cell value).
     */
    private List<Map<String, String>> parseExcel(MultipartFile file) {
        List<Map<String, String>> result = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null)
                return result;

            // Read headers
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            // Read data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean hasData = false;

                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = cell != null ? getCellValueAsString(cell).trim() : "";
                    if (!value.isEmpty())
                        hasData = true;
                    rowMap.put(headers.get(j), value);
                }

                if (hasData) {
                    result.add(rowMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield date.toString(); // yyyy-MM-dd
                }
                // Avoid scientific notation for large numbers (e.g. ISBN)
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }

    private String getRequired(Map<String, String> row, String key, int rowNum) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Missing required field: " + key);
        }
        return value;
    }

    private String getOptional(Map<String, String> row, String key) {
        String value = row.get(key);
        return (value != null && !value.isBlank()) ? value : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            // Try other common formats
            try {
                String[] parts = dateStr.split("[/\\-]");
                if (parts.length == 3) {
                    int a = Integer.parseInt(parts[0]);
                    int b = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    // Assume dd/MM/yyyy or MM/dd/yyyy
                    if (c > 100) { // Year is last
                        if (b > 12) { // b is day, a is month → MM/dd/yyyy
                            return LocalDate.of(c, a, b);
                        }
                        return LocalDate.of(c, b, a); // dd/MM/yyyy
                    } else if (a > 100) { // yyyy/MM/dd
                        return LocalDate.of(a, b, c);
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    private BulkUploadResponse buildResponse(int total, int success, List<String> errors) {
        return BulkUploadResponse.builder()
                .total(total)
                .success(success)
                .failed(total - success)
                .errors(errors)
                .build();
    }
}
