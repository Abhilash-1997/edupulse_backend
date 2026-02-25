package com.school.management.spec;

import com.school.management.constant.BusTripStatus;
import com.school.management.entity.BusTrip;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BusTripSpecification {
    public static Specification<BusTrip> withFilters(
            UUID schoolId, UUID busId,
            BusTripStatus status,
            LocalDateTime startDate, LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (busId != null)
                predicates.add(cb.equal(root.get("bus").get("id"), busId));

            if (status != null)
                predicates.add(cb.equal(root.get("status"), status));

            if (startDate != null && endDate != null)
                predicates.add(cb.between(root.get("startTime"), startDate, endDate));

            query.orderBy(cb.desc(root.get("startTime")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
