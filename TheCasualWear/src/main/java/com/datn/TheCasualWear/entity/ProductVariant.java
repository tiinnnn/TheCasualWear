package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity @Table(name = "product_variant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "cost_price", nullable = false)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "price_adjustment")
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<VariantImage> images;

    @Transient
    public BigDecimal getActualPrice() {
        return product.getPrice().add(
                priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO
        );
    }
}