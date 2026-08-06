package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "pos_counter")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PosCounter {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 20)
    private String code; // mã quầy, VD: Q1, Q2

    @Column(length = 150)
    private String name; // tên/vị trí, VD: "Quầy 1 - gần cửa ra vào"

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}