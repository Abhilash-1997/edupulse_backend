package com.school.management.repository;

import com.school.management.constant.PaymentStatus;
import com.school.management.entity.FeePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
                        Pageable pageable);

        Page<FeePayment> findBySchool_IdAndStudent_IdAndStatusAndDeletedAtIsNull(
                        UUID schoolId,
                        UUID studentId,
                        PaymentStatus status,
                        Pageable pageable);

        @Query("""
                        SELECT fp FROM FeePayment fp
                        JOIN FETCH fp.feeStructure
                        WHERE fp.student.id = :studentId
                          AND fp.school.id = :schoolId
                          AND fp.deletedAt IS NULL
                        ORDER BY fp.paymentDate DESC
                        """)
        List<FeePayment> findByStudentIdAndSchoolIdWithFeeStructure(
                        @Param("studentId") UUID studentId,
                        @Param("schoolId") UUID schoolId);

    @Query("""
    select fp from FeePayment fp
    join fetch fp.school
    join fetch fp.student s
    left join fetch s.parent p
    left join fetch p.user
    left join fetch s.classEntity ce
    left join fetch s.section
    join fetch fp.feeStructure
    where fp.id = :paymentId
      and fp.school.id = :schoolId
      and fp.deletedAt is null
""")
    Optional<FeePayment> findForReceipt(
            @Param("paymentId") UUID paymentId,
            @Param("schoolId") UUID schoolId
    );

}
