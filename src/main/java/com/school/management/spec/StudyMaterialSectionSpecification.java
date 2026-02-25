package com.school.management.spec;

import com.school.management.entity.StudyMaterialSection;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudyMaterialSectionSpecification {
    public static Specification<StudyMaterialSection> withFilters(
            UUID schoolId, UUID classId,
            UUID subjectId, UUID sectionId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (classId != null)
                predicates.add(cb.equal(root.get("classEntity").get("id"), classId));

            if (subjectId != null)
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));

            if (sectionId != null)
                predicates.add(cb.equal(root.get("section").get("id"), sectionId));

            query.orderBy(
                    cb.asc(root.get("order")),
                    cb.desc(root.get("createdAt"))
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
