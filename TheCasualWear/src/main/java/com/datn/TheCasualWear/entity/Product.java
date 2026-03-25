package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @jakarta.validation.constraints.Size(min = 3, max = 100, message = "Tên sản phẩm phải từ 3-100 ký tự")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "sku", unique = true, length = 50)
    private String sku;

    @NotNull(message = "Giá bán không được null")
    @Positive(message = "Giá bán phải lớn hơn 0")
    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được null")
    @Min(value = 0, message = "Tồn kho không được âm")
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @NotNull(message = "Giá vốn không được null")
    @PositiveOrZero(message = "Giá vốn phải >= 0")
    @Column(name = "cost_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne
    @JoinColumn(name = "color_id")
    private Color color;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt = new Date();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}
