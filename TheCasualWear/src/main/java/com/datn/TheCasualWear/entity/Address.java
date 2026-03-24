package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String district;
    private String country = "Vietnam";
    private Boolean isDefault = false;
}

