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

    public CounterCartItemDTO() {}

    public CounterCartItemDTO(Integer variantId, String productName, String sizeName,
                              String colorName, String sku, BigDecimal unitPrice,
                              Integer quantity, Integer availableStock, String imageUrl) {
        this.variantId = variantId;
        this.productName = productName;
        this.sizeName = sizeName;
        this.colorName = colorName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.availableStock = availableStock;
        this.imageUrl = imageUrl;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

}