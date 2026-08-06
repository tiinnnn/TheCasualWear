package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "order_detail")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private AppOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)   // ← thêm mới
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    // Giá thực khách trả (đã áp sale nếu sản phẩm đang có sale tại thời điểm mua)
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    // MỚI: giá gốc của sản phẩm tại thời điểm mua (chưa áp sale). Snapshot
    // độc lập với bảng product_sale — không cần chặn xóa sale cũ, vì đơn
    // hàng đã tự chứa đủ thông tin để biết có sale hay không:
    //   originalPrice == price  → mua giá thường
    //   originalPrice >  price  → mua lúc đang có sale, % giảm tự tính được
    @Column(name = "original_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalPrice;
}