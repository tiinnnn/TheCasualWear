package com.datn.TheCasualWear.dto;

import java.math.BigDecimal;

// Số tổng hợp các ca ĐÃ CHỐT (CLOSED) trong 1 ngày — không bao gồm ca
// đang OPEN (xem riêng ở phần "đang diễn ra, tạm tính").
public record DailySummaryDTO(
        BigDecimal totalRevenue,       // tổng doanh thu mọi phương thức thanh toán
        int totalItemsSold,
        int shiftCount,
        BigDecimal totalCashDifference, // cộng dồn chênh lệch tiền mặt mọi ca
        int mismatchCount               // số ca có cashDifference != 0
) {}
