package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    List<Voucher> findByIsActiveTrueAndEndDateAfter(LocalDateTime now);

    @Modifying
    @Query("UPDATE Voucher v SET v.isActive = false WHERE v.endDate < :dateTime AND v.isActive = true")
    void deactivateExpiredVouchers(@Param("dateTime") LocalDateTime dateTime);

}
