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

    // Tổng số lượng sản phẩm đã bán trong ca tính đến hiện tại (đơn COMPLETED).
    // Dùng chung cho preview lúc mở form đóng ca và chốt chính thức lúc closeShift().
    public int previewItemsSold(Shift shift) {
        Integer count = orderRepository.sumItemQuantityByShiftId(shift.getId());
        return count != null ? count : 0;
    }

    /**
     * Đóng ca: tự tính expectedCash + itemsSoldCount, "chốt cứng" cả 2 số
     * này vào ca (dùng để cashier ca sau đối chiếu khi xác nhận bàn giao).
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
        shift.setItemsSoldCount(previewItemsSold(shift));
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
        shift.setNote(note);

        return shiftRepository.save(shift);
    }

    // ─────────────────────────────────────────────────────────────
    // XÁC NHẬN BÀN GIAO CA (handover confirmation)
    // ─────────────────────────────────────────────────────────────

    // Ca CLOSED gần nhất (toàn hệ thống) mà chưa ai xác nhận bàn giao —
    // nếu có, cashier chuẩn bị mở ca mới phải xem & xác nhận số liệu ca
    // này trước, chặn không cho mở ca tiếp cho tới khi xác nhận xong.
    public Optional<Shift> getLatestUnconfirmedClosedShift() {
        return shiftRepository.findFirstByStatusAndHandoverConfirmedByIsNullOrderByClosedAtDesc(
                ShiftStatus.CLOSED);
    }

    /**
     * @param isMatch true = cashier ca sau xác nhận số liệu ca trước đúng;
     *                false = báo có sai lệch (bắt buộc kèm note giải thích).
     */
    @Transactional
    public Shift confirmHandover(Integer shiftId, AppUser confirmingUser,
                                 boolean isMatch, String note) {
        Shift shift = getShiftById(shiftId);

        if (shift.getStatus() != ShiftStatus.CLOSED) {
            throw new IllegalStateException("Ca này chưa được đóng, không thể xác nhận bàn giao!");
        }
        if (shift.isHandoverConfirmed()) {
            throw new IllegalStateException("Ca này đã được xác nhận bàn giao trước đó!");
        }
        if (!isMatch && (note == null || note.isBlank())) {
            throw new IllegalArgumentException(
                    "Vui lòng ghi rõ sai lệch cụ thể khi báo cáo số liệu không khớp!");
        }

        shift.setHandoverConfirmedBy(confirmingUser);
        shift.setHandoverConfirmedAt(LocalDateTime.now());
        shift.setHandoverNote(isMatch ? note : "[SAI LỆCH] " + note);

        return shiftRepository.save(shift);
    }

    public List<Shift> getHistory(AppUser cashier) {
        return shiftRepository.findByCashierIdOrderByOpenedAtDesc(cashier.getId());
    }

    public List<Shift> getAllHistory() {
        return shiftRepository.findAllByOrderByOpenedAtDesc();
    }
}