package com.school.management.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public class SpecificationUtils {
    public static <T> Predicate nullableEquals(
            CriteriaBuilder cb, Expression<?> path, Object value) {
        if (value == null) return cb.conjunction(); // always true = ignored
        return cb.equal(path, value);
    }

    public static <T> Predicate nullableBetween(
            CriteriaBuilder cb,
            Expression<? extends Comparable> path,
            Comparable start,
            Comparable end) {
        if (start == null || end == null) return cb.conjunction();
        return cb.between(path, start, end);
    }
}
