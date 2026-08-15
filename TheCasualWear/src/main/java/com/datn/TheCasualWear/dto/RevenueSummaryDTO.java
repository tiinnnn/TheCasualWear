package com.datn.TheCasualWear.dto;

import java.math.BigDecimal;

/**
 * Doanh thu/lợi nhuận theo khoảng thời gian tùy chọn (mục 2.4/2.5).
 * Công thức: totalRevenue = posRevenue + onlineVnpayRevenue + onlineCodRevenue
 *            totalProfit  = totalRevenue - totalCost
 * Xem OrderService.getRevenueSummary() và OrderService.resolveDateRange().
 */
public record RevenueSummaryDTO(
        BigDecimal posRevenue,
        long posOrders,

        BigDecimal onlineVnpayRevenue,
        long onlineVnpayOrders,

        BigDecimal onlineCodRevenue,
        long onlineCodOrders,

        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal totalProfit
) {
}
