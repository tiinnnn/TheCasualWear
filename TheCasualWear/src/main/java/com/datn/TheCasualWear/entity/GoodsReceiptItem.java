package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "goods_receipt_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GoodsReceiptItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    // Giá nhập snapshot tại thời điểm nhập — không lấy trực tiếp từ
    // ProductVariant.costPrice vì giá nhập có thể đổi qua từng đợt.
    @Column(name = "unit_cost_price", nullable = false)
    private BigDecimal unitCostPrice = BigDecimal.ZERO;
}
