package com.datn.TheCasualWear.repository;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {

    // Dùng để kiểm tra + lấy ca đang mở của 1 cashier (tối đa 1 ca OPEN/người,
    // ràng buộc bởi unique filtered index ở tầng DB)
    Optional<Shift> findByCashierIdAndStatus(Integer cashierId, ShiftStatus status);

    List<Shift> findByCashierIdOrderByOpenedAtDesc(Integer cashierId);

    // Lịch sử ca của 1 cashier, giới hạn trong khoảng thời gian (dùng cho
    // "lịch sử ca trong tuần" phía cashier).
    List<Shift> findByCashierIdAndOpenedAtBetweenOrderByOpenedAtDesc(
            Integer cashierId, LocalDateTime from, LocalDateTime to);

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

    // Bộ lọc trang danh sách ca (Admin) — mọi tham số đều optional, truyền
    // null nghĩa là bỏ qua điều kiện đó.
    @Query("SELECT s FROM Shift s WHERE " +
            "(:counterId IS NULL OR s.counter.id = :counterId) AND " +
            "(:cashierId IS NULL OR s.cashier.id = :cashierId) AND " +
            "(:from IS NULL OR s.openedAt >= :from) AND " +
            "(:to IS NULL OR s.openedAt <= :to) " +
            "ORDER BY s.openedAt DESC")
    List<Shift> findFiltered(@Param("counterId") Integer counterId,
                             @Param("cashierId") Integer cashierId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    // Danh sách cashier từng mở ca — dùng đổ vào dropdown "Nhân viên" của bộ lọc.
    @Query("SELECT DISTINCT s.cashier FROM Shift s ORDER BY s.cashier.username")
    List<AppUser> findDistinctCashiers();
}