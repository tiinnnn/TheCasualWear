package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // MỚI (4.5): cân nặng (gram) — bắt buộc cho GHN Calculate Fee. Default
    // 300g khớp với giá trị default ở migration SQL cho sản phẩm cũ.
    @Column(nullable = false)
    private Integer weight = 300;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // MỚI: lịch sử các đợt sale của sản phẩm này (có thể có nhiều đợt
    // theo thời gian, chỉ 1 đợt được coi là "đang chạy" tại 1 thời điểm —
    // xem ProductSaleService.getActiveSale())
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSale> sales = new ArrayList<>();

    @ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
    private Set<Collection> collections = new HashSet<>();
}