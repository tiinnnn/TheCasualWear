package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.OrderVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderVoucherRepository  extends JpaRepository<OrderVoucher, Integer> {
    // Kiểm tra user đã dùng voucher này chưa
    boolean existsByCustomerIdAndVoucherId(Integer customerId, Integer voucherId);

    Optional<OrderVoucher> findByOrderId(Integer orderId);
    boolean existsByVoucherId(Integer voucherId);
}
