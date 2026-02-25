package com.esprit.planning.repository;

import com.esprit.planning.entity.ProgressUpdate;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JPA Specification for filtering ProgressUpdate by projectId, freelancerId, contractId,
 * progress range, date range, and optional text search on title/description.
 */
public final class ProgressUpdateSpecification {

    private ProgressUpdateSpecification() {
    }

    public static Specification<ProgressUpdate> filtered(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            Optional<Long> contractId,
            Optional<Integer> progressMin,
            Optional<Integer> progressMax,
            Optional<LocalDate> dateFrom,
            Optional<LocalDate> dateTo,
            Optional<String> search) {
        return (Root<ProgressUpdate> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            projectId.ifPresent(id -> predicates.add(cb.equal(root.get("projectId"), id)));
            freelancerId.ifPresent(id -> predicates.add(cb.equal(root.get("freelancerId"), id)));
            contractId.ifPresent(id -> predicates.add(cb.equal(root.get("contractId"), id)));
            progressMin.ifPresent(min -> predicates.add(cb.greaterThanOrEqualTo(root.get("progressPercentage"), min)));
            progressMax.ifPresent(max -> predicates.add(cb.lessThanOrEqualTo(root.get("progressPercentage"), max)));

            dateFrom.ifPresent(from -> {
                LocalDateTime startOfDay = from.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            });
            dateTo.ifPresent(to -> {
                LocalDateTime endOfDay = to.atTime(23, 59, 59, 999_999_999);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            });

            search.filter(s -> s != null && !s.isBlank()).ifPresent(keyword -> {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.and(
                        cb.isNotNull(root.get("description")),
                        cb.like(cb.lower(root.get("description")), pattern));
                predicates.add(cb.or(titleLike, descLike));
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
