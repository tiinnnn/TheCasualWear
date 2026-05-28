package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "variant_image")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class VariantImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    private String imageUrl;
    private Integer sortOrder;
}
