package com.datn.TheCasualWear.enums;

public enum OrderStatus {
    PENDING,    // chờ xác nhận
    CONFIRMED,  // admin đã xác nhận
    SHIPPING,   // đang giao
    DELIVERED,  // đã giao
    COMPLETED,  // customer xác nhận
    CANCELLED   // đã hủy
}