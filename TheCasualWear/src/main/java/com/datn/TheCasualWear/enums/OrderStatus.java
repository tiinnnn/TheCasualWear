package com.datn.TheCasualWear.enums;

public enum OrderStatus {
    PENDING,    // chờ xác nhận
    CONFIRMED,  // admin đã xác nhận
    SHIPPING,   // đang giao
    COMPLETED,  // admin xác nhận đã giao xong (kiểm tra trên GHN)
    CANCELLED,  // đã hủy (trước khi hoặc trong khi giao, chưa hoàn thành)
    RETURNED    // đã hoàn hàng (sau khi hoàn thành, khách trả lại)
}