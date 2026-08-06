package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Sale/giảm giá theo lịch trình cho 1 sản phẩm. Có thể có nhiều bản ghi
// cho cùng 1 sản phẩm (lịch sử các đợt sale), chỉ bản ghi thỏa
// isCurrentlyRunning() mới được áp dụng khi tính giá.
@Entity @Table(name = "product_sale")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductSale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    public boolean isCurrentlyRunning() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive)
                && !now.isBefore(startDate)
                && !now.isAfter(endDate);
    }
}