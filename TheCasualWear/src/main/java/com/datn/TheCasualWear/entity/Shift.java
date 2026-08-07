package com.datn.TheCasualWear.entity;

import com.datn.TheCasualWear.enums.ShiftStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "shift")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Shift {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private AppUser cashier;

    // Quầy vật lý mà ca này diễn ra — bắt buộc chọn khi mở ca (Việc 2).
    // Nullable ở tầng DB vì các ca cũ (trước khi có tính năng quầy) không có.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_id")
    private PosCounter counter;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Tiền quỹ đầu ca — cashier tự đếm và nhập khi mở ca
    @Column(name = "opening_cash", nullable = false)
    private BigDecimal openingCash = BigDecimal.ZERO;

    // Hệ thống tự tính lúc đóng ca = openingCash + tổng thu tiền mặt trong ca
    @Column(name = "expected_cash")
    private BigDecimal expectedCash;

    // Cashier đếm thực tế lúc đóng ca
    @Column(name = "actual_cash")
    private BigDecimal actualCash;

    // actualCash - expectedCash (dương = dư, âm = thiếu)
    @Column(name = "cash_difference")
    private BigDecimal cashDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    @Column(length = 500)
    private String note;

    // ── MỚI: xác nhận bàn giao ca (handover confirmation) ──────────────────

    // Tổng số lượng sản phẩm đã bán trong ca — tự tính và "chốt cứng" tại
    // thời điểm đóng ca (song song với expectedCash), không thay đổi được
    // sau đó, dùng để cashier ca sau đối chiếu khi xác nhận bàn giao.
    @Column(name = "items_sold_count")
    private Integer itemsSoldCount;

    // Cashier của ca SAU xác nhận số liệu ca này (tiền + số lượng) là đúng.
    // NULL nghĩa là ca đã đóng nhưng CHƯA được ca kế tiếp xác nhận bàn giao.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handover_confirmed_by")
    private AppUser handoverConfirmedBy;

    @Column(name = "handover_confirmed_at")
    private LocalDateTime handoverConfirmedAt;

    // Ghi chú của cashier ca sau nếu phát hiện sai lệch lúc xác nhận bàn giao
    @Column(name = "handover_note", length = 500)
    private String handoverNote;

    @Transient
    public boolean isHandoverConfirmed() {
        return handoverConfirmedBy != null;
    }

    @Transient
    public boolean isOpen() {
        return status == ShiftStatus.OPEN;
    }
}