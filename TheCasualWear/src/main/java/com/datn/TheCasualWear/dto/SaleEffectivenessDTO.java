package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.ProductSale;

import java.math.BigDecimal;

/**
 * qtySold/revenue/discountGiven được tính trong đúng khoảng [sale.startDate,
 * sale.endDate] và chỉ tính đơn COMPLETED — không phụ thuộc sale còn
 * isActive hay không (đã hết hạn vẫn xem lại hiệu quả lịch sử được).
 */
public record SaleEffectivenessDTO(ProductSale sale, long qtySold,
                                    BigDecimal revenue, BigDecimal discountGiven) {
}
