package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.enums.OrderType;

import java.math.BigDecimal;

public record RevenueByChannelDTO(OrderType orderType, BigDecimal revenue, long orderCount) {
}
