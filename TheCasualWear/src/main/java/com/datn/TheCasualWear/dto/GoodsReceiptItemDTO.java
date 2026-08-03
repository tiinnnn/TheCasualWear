package com.datn.TheCasualWear.dto;

import java.math.BigDecimal;

// Dùng khi form tạo phiếu nhập kho gửi lên: mỗi dòng 1 variant + số lượng + giá nhập
public record GoodsReceiptItemDTO(
        Integer variantId,
        Integer quantity,
        BigDecimal unitCostPrice
) {}
