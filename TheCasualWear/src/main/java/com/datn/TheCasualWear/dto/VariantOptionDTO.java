package com.datn.TheCasualWear.dto;

// Trả về cho JS phía form tạo phiếu nhập khi admin chọn 1 Product,
// để hiển thị dropdown variant (size/màu/tồn hiện tại) mà không kéo cả
// entity ProductVariant (tránh lỗi lazy-loading khi serialize JSON).
public record VariantOptionDTO(
        Integer id,
        String sku,
        String sizeName,
        String colorName,
        Integer stock
) {}
