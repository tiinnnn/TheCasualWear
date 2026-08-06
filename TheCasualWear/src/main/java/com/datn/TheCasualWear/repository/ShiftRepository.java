package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {

    // Dùng để kiểm tra + lấy ca đang mở của 1 cashier (tối đa 1 ca OPEN/người,
    // ràng buộc bởi unique filtered index ở tầng DB)
    Optional<Shift> findByCashierIdAndStatus(Integer cashierId, ShiftStatus status);

    List<Shift> findByCashierIdOrderByOpenedAtDesc(Integer cashierId);

    List<Shift> findAllByOrderByOpenedAtDesc();

    // Ca đã CLOSED gần nhất (toàn hệ thống, không phân biệt cashier nào đóng)
    // mà chưa có ai ở ca sau xác nhận bàn giao — dùng để chặn mở ca mới cho
    // tới khi số liệu ca trước được xác nhận.
    Optional<Shift> findFirstByStatusAndHandoverConfirmedByIsNullOrderByClosedAtDesc(
            ShiftStatus status);
}