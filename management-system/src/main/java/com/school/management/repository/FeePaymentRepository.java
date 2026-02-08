package com.school.management.repository;

import com.school.management.constant.PaymentStatus;
import com.school.management.entity.FeePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeePaymentRepository extends BaseRepository<FeePayment> {

    Optional<FeePayment> findByIdAndDeletedAtIsNull(UUID id);

    Optional<FeePayment> findByIdAndSchool_IdAndDeletedAtIsNull(UUID id, UUID schoolId);

    List<FeePayment> findByStudent_IdAndSchool_IdAndDeletedAtIsNull(UUID studentId, UUID schoolId);

    List<FeePayment> findBySchool_IdAndDeletedAtIsNull(UUID schoolId);

    List<FeePayment> findBySchool_IdAndStatusAndDeletedAtIsNull(UUID schoolId, PaymentStatus status);

    Page<FeePayment> findBySchool_IdAndStatusAndDeletedAtIsNull(
            UUID schoolId,
            PaymentStatus status,
            Pageable pageable
    );

    Page<FeePayment> findBySchool_IdAndStudent_IdAndStatusAndDeletedAtIsNull(
            UUID schoolId,
            UUID studentId,
            PaymentStatus status,
            Pageable pageable
    );
}