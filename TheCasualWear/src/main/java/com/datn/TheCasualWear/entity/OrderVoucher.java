package com.datn.TheCasualWear.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "OrderVoucher")
public class OrderVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;
}

