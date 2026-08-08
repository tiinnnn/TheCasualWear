package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.DailySummaryDTO;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.PosCounter;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.enums.ShiftStatus;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.PosCounterRepository;
import com.datn.TheCasualWear.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository     shiftRepository;
    private final AppOrderRepository  orderRepository;
    private final PosCounterRepository counterRepository;

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

    public boolean isCounterOccupied(Integer counterId) {
        return shiftRepository.findByCounterIdAndStatus(counterId, ShiftStatus.OPEN).isPresent();
    }

    @Transactional
    public Shift openShift(AppUser cashier, Integer counterId, BigDecimal openingCash) {
        if (hasOpenShift(cashier)) {
            throw new IllegalStateException(
                    "Bạn đang có 1 ca chưa đóng! Vui lòng đóng ca hiện tại trước khi mở ca mới.");
        }
        if (openingCash == null || openingCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tiền quỹ đầu ca không được âm!");
        }

        PosCounter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quầy!"));

        if (isCounterOccupied(counterId)) {
            throw new IllegalStateException(
                    "Quầy " + counter.getCode() + " đang có người sử dụng, vui lòng chọn quầy khác!");
        }

        Shift shift = new Shift();
        shift.setCashier(cashier);
        shift.setCounter(counter);
        shift.setOpeningCash(openingCash);
        shift.setOpenedAt(LocalDateTime.now());
        shift.setStatus(ShiftStatus.OPEN);
        return shiftRepository.save(shift);
    }

    public BigDecimal previewExpectedCash(Shift shift) {
        BigDecimal cashRevenue = orderRepository
                .sumTotalPriceByShiftIdAndPaymentMethod(shift.getId(), CASH_PAYMENT_METHOD);
        if (cashRevenue == null) cashRevenue = BigDecimal.ZERO;
        return shift.getOpeningCash().add(cashRevenue);
    }

    public int previewItemsSold(Shift shift) {
        Integer count = orderRepository.sumItemQuantityByShiftId(shift.getId());
        return count != null ? count : 0;
    }

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

    public Optional<Shift> getLatestUnconfirmedClosedShiftForCounter(Integer counterId) {
        return shiftRepository.findFirstByCounterIdAndStatusAndHandoverConfirmedByIsNullOrderByClosedAtDesc(
                counterId, ShiftStatus.CLOSED);
    }

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

    public List<com.datn.TheCasualWear.entity.AppOrder> getOrdersForShift(Integer shiftId) {
        return orderRepository.findByShiftIdOrderByOrderDateDesc(shiftId);
    }

    public List<Shift> getHistory(AppUser cashier) {
        return shiftRepository.findByCashierIdOrderByOpenedAtDesc(cashier.getId());
    }

    // Lịch sử ca của cashier CHỈ trong tuần hiện tại (thứ 2 → hôm nay).
    public List<Shift> getHistoryThisWeek(AppUser cashier) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDateTime from = startOfWeek.atStartOfDay();
        LocalDateTime to = today.atTime(23, 59, 59);
        return shiftRepository.findByCashierIdAndOpenedAtBetweenOrderByOpenedAtDesc(
                cashier.getId(), from, to);
    }

    public List<Shift> getAllHistory() {
        return shiftRepository.findAllByOrderByOpenedAtDesc();
    }

    // Danh sách ca (Admin) có lọc theo quầy / nhân viên / khoảng thời gian —
    // mọi tham số đều optional, truyền null để bỏ qua điều kiện đó.
    public List<Shift> getFilteredHistory(Integer counterId, Integer cashierId,
                                          LocalDateTime from, LocalDateTime to) {
        return shiftRepository.findFiltered(counterId, cashierId, from, to);
    }

    // Danh sách cashier từng mở ca — đổ vào dropdown "Nhân viên" của bộ lọc.
    public List<AppUser> getAllCashiers() {
        return shiftRepository.findDistinctCashiers();
    }

    public List<Shift> getShiftsClosedOnDate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(23, 59, 59);
        return shiftRepository.findByStatusAndClosedAtBetweenOrderByClosedAtDesc(
                ShiftStatus.CLOSED, from, to);
    }

    public List<Shift> getAllOpenShifts() {
        return shiftRepository.findByStatus(ShiftStatus.OPEN);
    }

    public DailySummaryDTO getDailySummary(LocalDate date) {
        List<Shift> closedShifts = getShiftsClosedOnDate(date);

        List<Integer> shiftIds = closedShifts.stream().map(Shift::getId).toList();
        BigDecimal totalRevenue = shiftIds.isEmpty()
                ? BigDecimal.ZERO
                : orderRepository.sumTotalPriceByShiftIds(shiftIds);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        int totalItemsSold = closedShifts.stream()
                .mapToInt(s -> s.getItemsSoldCount() != null ? s.getItemsSoldCount() : 0)
                .sum();

        BigDecimal totalCashDifference = closedShifts.stream()
                .map(s -> s.getCashDifference() != null ? s.getCashDifference() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int mismatchCount = (int) closedShifts.stream()
                .filter(s -> s.getCashDifference() != null && s.getCashDifference().signum() != 0)
                .count();

        return new DailySummaryDTO(totalRevenue, totalItemsSold, closedShifts.size(),
                totalCashDifference, mismatchCount);
    }
}