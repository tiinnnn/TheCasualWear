package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OrderListDTO {

    private final Integer       id;
    private final String        customerUsername;
    private final String        customerPhone;
    private final LocalDateTime orderDate;
    private final BigDecimal    totalPrice;
    private final OrderStatus   status;
    private final int           failCounts;   // so lan giao that bai
    private final String        deliveryName; // ten nv dang giao (neu co)

    public OrderListDTO(AppOrder order, int failCounts, String deliveryName) {
        this.id               = order.getId();
        this.customerUsername = order.getCustomer().getUsername();
        this.customerPhone    = order.getShippingAddress() != null
                ? order.getShippingAddress().getPhone() : "";
        this.orderDate        = order.getOrderDate();
        this.totalPrice       = order.getTotalPrice();
        this.status           = order.getStatus();
        this.failCounts       = failCounts;
        this.deliveryName     = deliveryName;
    }
}