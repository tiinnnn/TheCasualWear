package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.ProductVariant;

/**
 * Ném khi tạo variant nhưng đã có variant khác cùng size + màu cho cùng
 * sản phẩm. KHÔNG coi đây là lỗi validate như SKU trùng — mà mang theo
 * variant cũ tìm được để tầng controller gom vào danh sách chuyển sang
 * trang nhập kho (Goods Receipt), cho admin bổ sung số lượng cho đúng
 * variant đó thay vì báo lỗi rồi bỏ qua.
 */
public class DuplicateVariantException extends RuntimeException {

    private final ProductVariant existingVariant;

    public DuplicateVariantException(ProductVariant existingVariant) {
        super("Biến thể (size + màu) này đã tồn tại cho sản phẩm.");
        this.existingVariant = existingVariant;
    }

    public ProductVariant getExistingVariant() {
        return existingVariant;
    }
}
