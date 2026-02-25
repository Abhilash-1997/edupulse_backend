package com.school.management.spec;

import com.school.management.entity.ExamResult;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExamResultSpecification {
    public static Specification<ExamResult> withFilters(
            UUID schoolId, UUID examId,
            UUID subjectId, UUID classId, UUID sectionId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (examId != null)
                predicates.add(cb.equal(root.get("exam").get("id"), examId));

            if (subjectId != null)
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));

            if (classId != null)
                predicates.add(cb.equal(root.get("student").get("classEntity").get("id"), classId));

            if (sectionId != null)
                predicates.add(cb.equal(root.get("student").get("section").get("id"), sectionId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
