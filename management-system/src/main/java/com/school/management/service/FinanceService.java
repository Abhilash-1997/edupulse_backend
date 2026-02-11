package com.school.management.service;

import com.school.management.constant.PaymentMethod;
import com.school.management.constant.PaymentStatus;
import com.school.management.constant.UserRole;
import com.school.management.dto.request.CollectFeeRequest;
import com.school.management.dto.request.CreateFeeStructureRequest;
import com.school.management.dto.request.ProcessPaymentRequest;
import com.school.management.dto.response.*;
import com.school.management.entity.*;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

        private final FeeStructureRepository feeStructureRepository;
        private final FeePaymentRepository feePaymentRepository;
        private final ClassRepository classRepository;
        private final StudentRepository studentRepository;
        private final ParentRepository parentRepository;
        private final SchoolRepository schoolRepository;
        private final PdfGenerationService pdfGeneratorService;
        private final EmailService emailService;

        // ============================ CREATE FEE STRUCTURE
        // ===============================================

        @Transactional
        public FeeStructureResponse createFeeStructure(CreateFeeStructureRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                ClassEntity classEntity = classRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getClassId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

                FeeStructure feeStructure = FeeStructure.builder()
                                .school(school)
                                .classEntity(classEntity)
                                .name(request.getName())
                                .amount(request.getAmount())
                                .frequency(request.getFrequency())
                                .dueDate(request.getDueDate())
                                .build();

                feeStructure = feeStructureRepository.save(feeStructure);

                return mapToFeeStructureResponse(feeStructure);
        }

        // ============================ GET FEE STRUCTURE
        // ===============================================

        @Transactional(readOnly = true)
        public List<FeeStructureResponse> getFeeStructures(UUID classId) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                List<FeeStructure> feeStructures;

                if (classId != null) {
                        feeStructures = feeStructureRepository
                                        .findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId, classId);
                } else {
                        feeStructures = feeStructureRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
                }

                return feeStructures.stream()
                                .map(this::mapToFeeStructureResponse)
                                .collect(Collectors.toList());
        }

        // ============================ COLLECT FEE
        // ===============================================

        @Transactional
        public FeePaymentResponse collectFee(CollectFeeRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getStudentId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                FeeStructure feeStructure = feeStructureRepository
                                .findByIdAndSchool_IdAndDeletedAtIsNull(request.getFeeStructureId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

                // Use amount or amountPaid
                Float amount = request.getAmountPaid() != null ? request.getAmountPaid() : request.getAmount();

                FeePayment feePayment = FeePayment.builder()
                                .school(school)
                                .student(student)
                                .feeStructure(feeStructure)
                                .amountPaid(amount)
                                .paymentDate(LocalDate.now())
                                .transactionId(request.getTransactionId())
                                .paymentMethod(request.getPaymentMethod())
                                .status(PaymentStatus.SUCCESS)
                                .build();

                feePayment = feePaymentRepository.save(feePayment);

                return mapToFeePaymentResponse(feePayment);
        }

        // ============================ PROCESS PAYMENT
        // ===============================================

        @Transactional
        public FeePaymentResponse processPayment(ProcessPaymentRequest request) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                Student student = studentRepository.findByIdAndSchool_IdAndDeletedAtIsNull(
                                request.getStudentId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                FeeStructure feeStructure = feeStructureRepository
                                .findByIdAndSchool_IdAndDeletedAtIsNull(request.getFeeStructureId(), schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Fee structure not found"));

                // Dummy payment processing (always SUCCESS)
                String transactionId = "TXN" + System.currentTimeMillis();

                FeePayment feePayment = FeePayment.builder()
                                .school(school)
                                .student(student)
                                .feeStructure(feeStructure)
                                .amountPaid(request.getAmountPaid())
                                .paymentDate(LocalDate.now())
                                .transactionId(transactionId)
                                .paymentMethod(PaymentMethod.ONLINE)
                                .status(PaymentStatus.SUCCESS)
                                .build();

                feePayment = feePaymentRepository.save(feePayment);

                // Generate PDF receipt
                try {
                        byte[] receiptPdf = pdfGeneratorService.generateFeeReceipt(feePayment);

                        // Send email
                        emailService.sendFeePaymentReceiptEmail(
                                        student.getParent().getUser().getEmail(),
                                        feeStructure.getName(),
                                        receiptPdf,
                                        school.getName());
                } catch (Exception e) {
                        log.error("Failed to generate/send receipt", e);
                }

                return mapToFeePaymentResponse(feePayment);
        }

        // ============================ GET FEE STATISTICS
        // ===============================================

        @Transactional(readOnly = true)
        public FeeStatisticsResponse getFeeStatistics() {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                List<ClassEntity> classes = classRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

                Float overallTotalFees = 0.0f;
                Float overallTotalCollected = 0.0f;
                List<FeeStatisticsResponse.ClassStats> classSummary = new ArrayList<>();

                for (ClassEntity classEntity : classes) {
                        // Get all fee structures for this class
                        List<FeeStructure> feeStructures = feeStructureRepository
                                        .findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId,
                                                        classEntity.getId());

                        // Get all students in this class
                        List<Student> students = studentRepository
                                        .findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId,
                                                        classEntity.getId());

                        int studentCount = students.size();

                        // Calculate total collectible
                        Float classTotalFees = feeStructures.stream()
                                        .map(fee -> fee.getAmount() * studentCount)
                                        .reduce(0.0f, Float::sum);

                        // Calculate collected amount
                        Float classCollected = 0.0f;
                        for (FeeStructure fee : feeStructures) {
                                List<FeePayment> payments = feePaymentRepository
                                                .findBySchool_IdAndStatusAndDeletedAtIsNull(schoolId,
                                                                PaymentStatus.SUCCESS);

                                classCollected += payments.stream()
                                                .filter(p -> p.getFeeStructure().getId().equals(fee.getId()))
                                                .map(FeePayment::getAmountPaid)
                                                .reduce(0.0f, Float::sum);
                        }

                        Float classPending = classTotalFees - classCollected;
                        Float collectionRate = classTotalFees > 0 ? (classCollected / classTotalFees) * 100 : 0.0f;

                        classSummary.add(FeeStatisticsResponse.ClassStats.builder()
                                        .classId(classEntity.getId().toString())
                                        .className(classEntity.getName())
                                        .studentCount(studentCount)
                                        .totalFees(classTotalFees)
                                        .collectedAmount(classCollected)
                                        .pendingAmount(classPending)
                                        .collectionRate(collectionRate)
                                        .build());

                        overallTotalFees += classTotalFees;
                        overallTotalCollected += classCollected;
                }

                Float overallPending = overallTotalFees - overallTotalCollected;
                Float overallCollectionRate = overallTotalFees > 0 ? (overallTotalCollected / overallTotalFees) * 100
                                : 0.0f;

                FeeStatisticsResponse.OverviewStats overview = FeeStatisticsResponse.OverviewStats.builder()
                                .totalFees(overallTotalFees)
                                .totalCollected(overallTotalCollected)
                                .totalPending(overallPending)
                                .overallCollectionRate(overallCollectionRate)
                                .build();

                return FeeStatisticsResponse.builder()
                                .overview(overview)
                                .classSummary(classSummary)
                                .build();
        }

        // ============================ GET CLASS FEE STATUS
        // ===============================================

        @Transactional(readOnly = true)
        public List<StudentFeeStatusResponse> getClassFeeStatus(UUID classId) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                List<Student> students = studentRepository.findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId,
                                classId);

                List<FeeStructure> feeStructures = feeStructureRepository
                                .findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId, classId);

                return students.stream()
                                .map(student -> {
                                        Float totalDue = feeStructures.stream()
                                                        .map(FeeStructure::getAmount)
                                                        .reduce(0.0f, Float::sum);

                                        Float totalPaid = feePaymentRepository
                                                        .findByStudent_IdAndSchool_IdAndDeletedAtIsNull(student.getId(),
                                                                        schoolId)
                                                        .stream()
                                                        .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                                                        .map(FeePayment::getAmountPaid)
                                                        .reduce(0.0f, Float::sum);

                                        String status;
                                        if (totalPaid.equals(totalDue)) {
                                                status = "PAID";
                                        } else if (totalPaid > 0) {
                                                status = "PARTIAL";
                                        } else {
                                                status = "PENDING";
                                        }

                                        return StudentFeeStatusResponse.builder()
                                                        .studentId(student.getId())
                                                        .studentName(student.getName())
                                                        .admissionNumber(student.getAdmissionNumber())
                                                        .totalDue(totalDue)
                                                        .totalPaid(totalPaid)
                                                        .balance(totalDue - totalPaid)
                                                        .status(status)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Page<FeePaymentResponse> getReceipts(UUID studentId, Integer page, Integer size) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                Pageable pageable = PageRequest.of(
                                page != null ? page : 0,
                                size != null ? size : 20);

                Page<FeePayment> payments;

                if (studentId != null) {
                        payments = feePaymentRepository.findBySchool_IdAndStudent_IdAndStatusAndDeletedAtIsNull(
                                        schoolId, studentId, PaymentStatus.SUCCESS, pageable);
                } else {
                        payments = feePaymentRepository.findBySchool_IdAndStatusAndDeletedAtIsNull(
                                        schoolId, PaymentStatus.SUCCESS, pageable);
                }

                return payments.map(this::mapToFeePaymentResponse);
        }

        // ============================ GET STUDENT FEE DETAILS
        // ===============================================

        @Transactional(readOnly = true)
        public StudentFeeDetailsResponse getStudentFeeDetails(UUID studentId) {
                UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

                // Verify student belongs to this school
                Student student = studentRepository.findByIdAndSchoolIdWithDetails(studentId, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                // If user is a parent, verify they have access to this student
                if (SecurityUtils.getCurrentUserRole() == UserRole.PARENT) {
                        Parent parent = parentRepository
                                        .findByUser_IdAndDeletedAtIsNull(SecurityUtils.getCurrentUserId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found"));

                        if (student.getParent() == null || !student.getParent().getId().equals(parent.getId())) {
                                throw new BadRequestException("You are not authorized to view this student's fees");
                        }
                }

                // Get fee structures for student's class
                List<FeeStructure> feeStructures = feeStructureRepository
                                .findBySchool_IdAndClassEntity_IdAndDeletedAtIsNull(schoolId,
                                                student.getClassEntity().getId());

                // Get all payments for this student (with FeeStructure eagerly loaded)
                List<FeePayment> payments = feePaymentRepository
                                .findByStudentIdAndSchoolIdWithFeeStructure(studentId, schoolId);

                // Calculate fee breakdown
                List<StudentFeeDetailsResponse.FeeBreakdownItem> feeBreakdown = feeStructures.stream()
                                .map(feeStructure -> {
                                        Float totalPaid = payments.stream()
                                                        .filter(p -> p.getFeeStructure().getId()
                                                                        .equals(feeStructure.getId()))
                                                        .map(FeePayment::getAmountPaid)
                                                        .reduce(0.0f, Float::sum);

                                        Float pending = feeStructure.getAmount() - totalPaid;

                                        String status;
                                        if (totalPaid == 0) {
                                                status = "PENDING";
                                        } else if (totalPaid >= feeStructure.getAmount()) {
                                                status = "PAID";
                                        } else {
                                                status = "PARTIAL";
                                        }

                                        return StudentFeeDetailsResponse.FeeBreakdownItem.builder()
                                                        .feeStructureId(feeStructure.getId())
                                                        .feeName(feeStructure.getName())
                                                        .amount(feeStructure.getAmount())
                                                        .frequency(feeStructure.getFrequency())
                                                        .totalPaid(totalPaid)
                                                        .pendingAmount(pending > 0 ? pending : 0.0f)
                                                        .status(status)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Calculate summary
                Float totalFees = feeStructures.stream()
                                .map(FeeStructure::getAmount)
                                .reduce(0.0f, Float::sum);
                Float totalPaid = payments.stream()
                                .map(FeePayment::getAmountPaid)
                                .reduce(0.0f, Float::sum);

                // Build student info
                String className = student.getClassEntity() != null
                                ? student.getClassEntity().getName()
                                                + (student.getSection() != null ? " - " + student.getSection().getName()
                                                                : "")
                                : "N/A";

                StudentFeeDetailsResponse.StudentInfo studentInfo = StudentFeeDetailsResponse.StudentInfo.builder()
                                .id(student.getId())
                                .name(student.getName())
                                .admissionNumber(student.getAdmissionNumber())
                                .className(className)
                                .guardianName(student.getParent() != null ? student.getParent().getGuardianName()
                                                : "N/A")
                                .contact(student.getParent() != null && student.getParent().getUser() != null
                                                ? student.getParent().getUser().getPhone()
                                                : "N/A")
                                .build();

                // Build payment history
                List<StudentFeeDetailsResponse.PaymentHistoryItem> paymentHistory = payments.stream()
                                .map(p -> StudentFeeDetailsResponse.PaymentHistoryItem.builder()
                                                .id(p.getId())
                                                .feeName(p.getFeeStructure() != null ? p.getFeeStructure().getName()
                                                                : null)
                                                .amountPaid(p.getAmountPaid())
                                                .paymentDate(p.getPaymentDate())
                                                .paymentMethod(p.getPaymentMethod())
                                                .transactionId(p.getTransactionId())
                                                .status(p.getStatus())
                                                .build())
                                .collect(Collectors.toList());

                return StudentFeeDetailsResponse.builder()
                                .student(studentInfo)
                                .summary(StudentFeeDetailsResponse.FeeSummary.builder()
                                                .totalFees(totalFees)
                                                .totalPaid(totalPaid)
                                                .totalPending(totalFees - totalPaid)
                                                .build())
                                .feeBreakdown(feeBreakdown)
                                .paymentHistory(paymentHistory)
                                .build();
        }

        private FeeStructureResponse mapToFeeStructureResponse(FeeStructure fee) {
                return FeeStructureResponse.builder()
                                .id(fee.getId())
                                .name(fee.getName())
                                .amount(fee.getAmount())
                                .frequency(fee.getFrequency())
                                .dueDate(fee.getDueDate())
                                .classId(fee.getClassEntity().getId())
                                .build();
        }

        private FeePaymentResponse mapToFeePaymentResponse(FeePayment payment) {
                return FeePaymentResponse.builder()
                                .id(payment.getId())
                                .amountPaid(payment.getAmountPaid())
                                .paymentDate(payment.getPaymentDate())
                                .transactionId(payment.getTransactionId())
                                .paymentMethod(payment.getPaymentMethod())
                                .status(payment.getStatus())
                                .studentId(payment.getStudent().getId())
                                .feeStructureId(payment.getFeeStructure().getId())
                                .feeName(payment.getFeeStructure().getName())
                                .build();
        }
}