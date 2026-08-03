package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "goods_receipt")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GoodsReceipt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mã phiếu hiển thị, vd: PN-20260803-001 (sinh ở Service, không phải id)
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Tổng giá trị phiếu — tính từ item khi tạo phiếu, lưu sẵn để hiển thị
    // danh sách phiếu mà không phải join tính lại mỗi lần.
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptItem> items = new ArrayList<>();
}
