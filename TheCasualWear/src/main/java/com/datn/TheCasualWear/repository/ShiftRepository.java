package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {

    // Dùng để kiểm tra + lấy ca đang mở của 1 cashier (tối đa 1 ca OPEN/người,
    // ràng buộc bởi unique filtered index ở tầng DB)
    Optional<Shift> findByCashierIdAndStatus(Integer cashierId, ShiftStatus status);

    List<Shift> findByCashierIdOrderByOpenedAtDesc(Integer cashierId);

    List<Shift> findAllByOrderByOpenedAtDesc();

    // Quầy này có đang bị ai chiếm (ca OPEN) không — dùng để chặn 2 cashier
    // cùng vào 1 quầy song song.
    Optional<Shift> findByCounterIdAndStatus(Integer counterId, ShiftStatus status);

    // Ca CLOSED gần nhất TẠI ĐÚNG QUẦY NÀY mà chưa ai xác nhận bàn giao —
    // scope theo counterId (KHÔNG còn toàn hệ thống nữa) để tránh cashier ở
    // quầy khác bị bắt xác nhận số liệu không liên quan tới mình.
    Optional<Shift> findFirstByCounterIdAndStatusAndHandoverConfirmedByIsNullOrderByClosedAtDesc(
            Integer counterId, ShiftStatus status);

    // Ca CLOSED trong khoảng thời gian (dùng cho báo cáo tổng kết cuối ngày,
    // lọc theo closedAt).
    List<Shift> findByStatusAndClosedAtBetweenOrderByClosedAtDesc(
            ShiftStatus status, LocalDateTime from, LocalDateTime to);

    // Mọi ca đang OPEN toàn hệ thống — phần "đang diễn ra, tạm tính" trong
    // báo cáo cuối ngày.
    List<Shift> findByStatus(ShiftStatus status);
}