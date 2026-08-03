package com.datn.TheCasualWear.entity;

import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mỗi dòng = 1 lần thay đổi stock của 1 variant.
// Được ghi tự động qua StockMovementLogService — không insert tay ở nơi khác,
// để đảm bảo mọi thay đổi stock đều có audit trail.
@Entity @Table(name = "stock_movement_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StockMovementLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private StockMovementType changeType;

    // Dương = tăng tồn (IMPORT, CANCEL, RETURN), âm = giảm tồn (SALE)
    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    // Số tồn SAU khi áp dụng thay đổi này — lưu sẵn để tra cứu nhanh
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 30)
    private StockRefType refType;

    // id của goods_receipt hoặc app_order tương ứng (tùy refType)
    @Column(name = "ref_id")
    private Integer refId;

    @Column(length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
