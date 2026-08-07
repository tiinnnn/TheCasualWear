package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.enums.StockMovementType;

/** totalQty luôn là số dương (đã lấy trị tuyệt đối) — chỉ thể hiện "tổng khối lượng biến động". */
public record StockMovementSummaryDTO(StockMovementType type, long totalQty) {
}
