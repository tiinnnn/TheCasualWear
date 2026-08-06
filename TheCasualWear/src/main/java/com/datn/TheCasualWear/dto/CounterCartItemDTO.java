package com.datn.TheCasualWear.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CounterCartItemDTO {

    private Integer variantId;
    private String  productName;
    private String  sizeName;
    private String  colorName;
    private String  sku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer availableStock; // để hiển thị cảnh báo, không trừ ngay
    private String  imageUrl;       // ảnh variant, fallback ảnh product nếu variant chưa có ảnh riêng

    // MỚI: thông tin sale — null nếu sản phẩm không có sale đang chạy.
    // originalPrice = giá gốc trước giảm, để hiện gạch ngang; discountPercent
    // để hiện badge "-X%". unitPrice ở trên vẫn luôn là giá thực khách trả
    // (đã áp sale), dùng cho mọi phép tính tiền — 2 field này chỉ để hiển thị.
    private BigDecimal originalPrice;
    private BigDecimal discountPercent;

    public CounterCartItemDTO() {}

    public CounterCartItemDTO(Integer variantId, String productName, String sizeName,
                              String colorName, String sku, BigDecimal unitPrice,
                              Integer quantity, Integer availableStock, String imageUrl) {
        this(variantId, productName, sizeName, colorName, sku, unitPrice,
                quantity, availableStock, imageUrl, null, null);
    }

    public CounterCartItemDTO(Integer variantId, String productName, String sizeName,
                              String colorName, String sku, BigDecimal unitPrice,
                              Integer quantity, Integer availableStock, String imageUrl,
                              BigDecimal originalPrice, BigDecimal discountPercent) {
        this.variantId = variantId;
        this.productName = productName;
        this.sizeName = sizeName;
        this.colorName = colorName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.availableStock = availableStock;
        this.imageUrl = imageUrl;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent;
    }

    public boolean isOnSale() {
        return discountPercent != null;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}