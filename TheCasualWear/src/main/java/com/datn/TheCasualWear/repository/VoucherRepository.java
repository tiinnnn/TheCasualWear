package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);
    List<Voucher> findByIsActiveTrueAndEndDateAfter(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Voucher v SET v.isActive = false " +
            "WHERE v.isActive = true " +
            "AND v.endDate IS NOT NULL " +
            "AND v.endDate < :now")
    void deactivateExpiredVouchers(@Param("now") LocalDateTime now);
    List<Voucher> findByIsActive(Boolean isActive);
}
