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

    @Transient
    public boolean isOpen() {
        return status == ShiftStatus.OPEN;
    }
}