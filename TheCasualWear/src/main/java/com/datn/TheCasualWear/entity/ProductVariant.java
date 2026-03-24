package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ProductVariant")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String sku;
    private BigDecimal price;
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne
    @JoinColumn(name = "color_id")
    private Color color;
}
