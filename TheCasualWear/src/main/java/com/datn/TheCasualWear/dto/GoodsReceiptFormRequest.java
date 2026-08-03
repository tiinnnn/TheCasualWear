package com.datn.TheCasualWear.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Backing object cho form tạo phiếu nhập kho (khác GoodsReceiptItemDTO ở chỗ
// đây là class mutable để Thymeleaf th:field bind được qua index items[0].xxx)
@Getter @Setter
public class GoodsReceiptFormRequest {

    private String supplierName;
    private String note;
    private List<GoodsReceiptItemForm> items = new ArrayList<>();

    @Getter @Setter
    public static class GoodsReceiptItemForm {
        private Integer variantId;
        private Integer quantity;
        private java.math.BigDecimal unitCostPrice;
    }
}
