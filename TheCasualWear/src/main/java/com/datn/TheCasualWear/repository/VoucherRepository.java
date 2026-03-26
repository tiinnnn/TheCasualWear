package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    // Lấy voucher còn hiệu lực
    List<Voucher> findByIsActiveTrueAndEndDateAfter(LocalDateTime now);
}
