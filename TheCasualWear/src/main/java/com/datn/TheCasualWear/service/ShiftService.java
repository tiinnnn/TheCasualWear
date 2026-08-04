package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.enums.ShiftStatus;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository     shiftRepository;
    private final AppOrderRepository  orderRepository;

    // Giá trị paymentMethod dùng để tính "tiền mặt thu trong ca" khi đóng ca.
    // ⚠️ CHỈNH LẠI chuỗi này cho khớp với giá trị thực tế mà form POS gửi lên
    // (xem tham số paymentMethod trong CashierService.checkout / trang POS —
    // ví dụ có thể là "CASH" hoặc "TIEN_MAT" tùy nhóm đặt tên).
    private static final String CASH_PAYMENT_METHOD = "CASH";

    public Optional<Shift> getOpenShift(AppUser cashier) {
        return shiftRepository.findByCashierIdAndStatus(cashier.getId(), ShiftStatus.OPEN);
    }

    public boolean hasOpenShift(AppUser cashier) {
        return getOpenShift(cashier).isPresent();
    }

    public Shift getOpenShiftOrThrow(AppUser cashier) {
        return getOpenShift(cashier)
                .orElseThrow(() -> new IllegalStateException(
                        "Bạn chưa mở ca làm việc! Vui lòng mở ca trước khi bán hàng."));
    }

    public Shift getShiftById(Integer id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm việc"));
    }

    @Transactional
    public Shift openShift(AppUser cashier, BigDecimal openingCash) {
        if (hasOpenShift(cashier)) {
            throw new IllegalStateException(
                    "Bạn đang có 1 ca chưa đóng! Vui lòng đóng ca hiện tại trước khi mở ca mới.");
        }
        if (openingCash == null || openingCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền quỹ đầu ca không được âm!");
        }

        Shift shift = new Shift();
        shift.setCashier(cashier);
        shift.setOpeningCash(openingCash);
        shift.setOpenedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.OPEN);
        return shiftRepository.save(shift);
    }

    // Tính "tiền mặt lẽ ra phải có" tại thời điểm gọi = openingCash + tổng
    // tiền mặt (CASH) đã thu trong ca tính đến hiện tại. Dùng chung cho:
    //  - Preview hiển thị gợi ý trên form đóng ca (chưa lưu gì)
    //  - Tính chính thức lúc closeShift() thực sự đóng ca
    public BigDecimal previewExpectedCash(Shift shift) {
        BigDecimal cashRevenue = orderRepository
                .sumTotalPriceByShiftIdAndPaymentMethod(shift.getId(), CASH_PAYMENT_METHOD);
        if (cashRevenue == null) cashRevenue = BigDecimal.ZERO;
        return shift.getOpeningCash().add(cashRevenue);
    }

    /**
     * Đóng ca: tự tính expectedCash = openingCash + tổng tiền mặt thu được
     * trong ca (dựa vào app_order.shift_id + paymentMethod = CASH), rồi so
     * sánh với actualCash cashier đếm thực tế để ra cashDifference.
     */
    @Transactional
    public Shift closeShift(Integer shiftId, AppUser actor, BigDecimal actualCash, String note) {
        Shift shift = getShiftById(shiftId);

        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("Ca này đã được đóng trước đó!");
        }
        if (!shift.getCashier().getId().equals(actor.getId())) {
            throw new IllegalStateException("Bạn không có quyền đóng ca của người khác!");
        }
        if (actualCash == null || actualCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền mặt thực đếm không được âm!");
        }

        BigDecimal expected = previewExpectedCash(shift);

        shift.setExpectedCash(expected);
        shift.setActualCash(actualCash);
        shift.setCashDifference(actualCash.subtract(expected));
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setNote(note);

        return shiftRepository.save(shift);
    }

    public List<Shift> getHistory(AppUser cashier) {
        return shiftRepository.findByCashierIdOrderByOpenedAtDesc(cashier.getId());
    }

    public List<Shift> getAllHistory() {
        return shiftRepository.findAllByOrderByOpenedAtDesc();
    }
}