package com.school.management.service;

import com.school.management.dto.request.CreateGradeRuleRequest;
import com.school.management.dto.response.GradeRuleResponse;
import com.school.management.entity.GradeRule;
import com.school.management.entity.School;
import com.school.management.exception.BadRequestException;
import com.school.management.exception.ResourceNotFoundException;
import com.school.management.repository.GradeRuleRepository;
import com.school.management.repository.SchoolRepository;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeRuleService {

    private final GradeRuleRepository gradeRuleRepository;
    private final SchoolRepository schoolRepository;

    /**
     * Create grade rule
     */
    @Transactional
    public GradeRuleResponse createGradeRule(CreateGradeRuleRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Validate percentage range
        if (request.getMinPercentage() < 0 || request.getMinPercentage() > 100) {
            throw new BadRequestException("Minimum percentage must be between 0 and 100");
        }
        if (request.getMaxPercentage() < 0 || request.getMaxPercentage() > 100) {
            throw new BadRequestException("Maximum percentage must be between 0 and 100");
        }
        if (request.getMinPercentage() >= request.getMaxPercentage()) {
            throw new BadRequestException("Minimum percentage must be less than maximum percentage");
        }

        // Check for overlapping ranges
        List<GradeRule> existingRules = gradeRuleRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        for (GradeRule existing : existingRules) {
            if (rangesOverlap(
                    request.getMinPercentage(), request.getMaxPercentage(),
                    existing.getMinPercentage(), existing.getMaxPercentage())) {
                throw new BadRequestException(
                        "Grade range overlaps with existing rule: " + existing.getGrade());
            }
        }

        GradeRule gradeRule = GradeRule.builder()
                .school(school)
                .grade(request.getGrade())
                .minPercentage(request.getMinPercentage())
                .maxPercentage(request.getMaxPercentage())
                .build();

        gradeRule = gradeRuleRepository.save(gradeRule);

        return mapToGradeRuleResponse(gradeRule);
    }

    /**
     * Bulk create grade rules
     */
    @Transactional
    public List<GradeRuleResponse> createGradeRules(List<CreateGradeRuleRequest> requests) {
        log.info("Requests to create grade rules for school: " + requests);
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        // Validate each request
        for (CreateGradeRuleRequest request : requests) {
            if (request.getMinPercentage() < 0 || request.getMinPercentage() > 100) {
                throw new BadRequestException(
                        "Minimum percentage must be between 0 and 100 for grade: " + request.getGrade());
            }
            if (request.getMaxPercentage() < 0 || request.getMaxPercentage() > 100) {
                throw new BadRequestException(
                        "Maximum percentage must be between 0 and 100 for grade: " + request.getGrade());
            }
            if (request.getMinPercentage() >= request.getMaxPercentage()) {
                throw new BadRequestException(
                        "Minimum percentage must be less than maximum percentage for grade: " + request.getGrade());
            }
        }

        // Check for overlaps within the incoming list itself
        for (int i = 0; i < requests.size(); i++) {
            for (int j = i + 1; j < requests.size(); j++) {
                if (rangesOverlap(
                        requests.get(i).getMinPercentage(), requests.get(i).getMaxPercentage(),
                        requests.get(j).getMinPercentage(), requests.get(j).getMaxPercentage())) {
                    throw new BadRequestException(
                            "Grade ranges overlap between: " + requests.get(i).getGrade() + " and "
                                    + requests.get(j).getGrade());
                }
            }
        }

        // Check for overlaps against existing rules in DB
        List<GradeRule> existingRules = gradeRuleRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
        for (CreateGradeRuleRequest request : requests) {
            for (GradeRule existing : existingRules) {
                if (rangesOverlap(
                        request.getMinPercentage(), request.getMaxPercentage(),
                        existing.getMinPercentage(), existing.getMaxPercentage())) {
                    throw new BadRequestException(
                            "Grade range for " + request.getGrade() + " overlaps with existing rule: "
                                    + existing.getGrade());
                }
            }
        }

        // Build and save all grade rules
        List<GradeRule> gradeRules = requests.stream()
                .map(request -> GradeRule.builder()
                        .school(school)
                        .grade(request.getGrade())
                        .minPercentage(request.getMinPercentage())
                        .maxPercentage(request.getMaxPercentage())
                        .description(request.getDescription())
                        .build())
                .collect(Collectors.toList());

        gradeRules = gradeRuleRepository.saveAll(gradeRules);

        return gradeRules.stream()
                .map(this::mapToGradeRuleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all grade rules
     */
    @Transactional(readOnly = true)
    public List<GradeRuleResponse> getGradeRules() {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        List<GradeRule> gradeRules = gradeRuleRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);

        return gradeRules.stream()
                .sorted((a, b) -> Float.compare(b.getMinPercentage(), a.getMinPercentage()))
                .map(this::mapToGradeRuleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get grade rule by ID
     */
    @Transactional(readOnly = true)
    public GradeRuleResponse getGradeRuleById(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        GradeRule gradeRule = gradeRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade rule not found"));

        if (!gradeRule.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Grade rule not found");
        }

        return mapToGradeRuleResponse(gradeRule);
    }

    /**
     * Update grade rule
     */
    @Transactional
    public GradeRuleResponse updateGradeRule(UUID id, CreateGradeRuleRequest request) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        GradeRule gradeRule = gradeRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade rule not found"));

        if (!gradeRule.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Grade rule not found");
        }

        // Validate new ranges if changed
        if (request.getMinPercentage() != null && request.getMaxPercentage() != null) {
            if (request.getMinPercentage() >= request.getMaxPercentage()) {
                throw new BadRequestException("Minimum percentage must be less than maximum percentage");
            }

            // Check for overlapping ranges (excluding current rule)
            List<GradeRule> existingRules = gradeRuleRepository.findBySchool_IdAndDeletedAtIsNull(schoolId);
            for (GradeRule existing : existingRules) {
                if (existing.getId().equals(id))
                    continue;

                if (rangesOverlap(
                        request.getMinPercentage(), request.getMaxPercentage(),
                        existing.getMinPercentage(), existing.getMaxPercentage())) {
                    throw new BadRequestException(
                            "Grade range overlaps with existing rule: " + existing.getGrade());
                }
            }
        }

        if (request.getGrade() != null) {
            gradeRule.setGrade(request.getGrade());
        }
        if (request.getMinPercentage() != null) {
            gradeRule.setMinPercentage(request.getMinPercentage());
        }
        if (request.getMaxPercentage() != null) {
            gradeRule.setMaxPercentage(request.getMaxPercentage());
        }

        gradeRule = gradeRuleRepository.save(gradeRule);

        return mapToGradeRuleResponse(gradeRule);
    }

    /**
     * Delete grade rule
     */
    @Transactional
    public void deleteGradeRule(UUID id) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        GradeRule gradeRule = gradeRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade rule not found"));

        if (!gradeRule.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Grade rule not found");
        }

        gradeRuleRepository.delete(gradeRule);
    }

    /**
     * Calculate grade for a percentage
     */
    @Transactional(readOnly = true)
    public String calculateGrade(Float percentage) {
        UUID schoolId = SecurityUtils.getCurrentUserSchoolId();

        return gradeRuleRepository.findBySchoolIdAndPercentage(schoolId, percentage)
                .map(GradeRule::getGrade)
                .orElse("N/A");
    }

    // ============= HELPER METHODS =============

    private boolean rangesOverlap(Float min1, Float max1, Float min2, Float max2) {
        return !(max1 <= min2 || max2 <= min1);
    }

    private GradeRuleResponse mapToGradeRuleResponse(GradeRule gradeRule) {
        return GradeRuleResponse.builder()
                .id(gradeRule.getId())
                .grade(gradeRule.getGrade())
                .minPercentage(gradeRule.getMinPercentage())
                .maxPercentage(gradeRule.getMaxPercentage())
                .description(gradeRule.getDescription())
                .build();
    }
}