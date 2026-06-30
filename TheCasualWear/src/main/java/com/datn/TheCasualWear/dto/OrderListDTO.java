package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
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
    private final String        trackingCode; // mã vận đơn GHN (nếu có)
    private final OrderType     orderType;    // ONLINE hoặc COUNTER

    public OrderListDTO(AppOrder order) {
        this.id               = order.getId();
        // customer có thể null nếu là khách vãng lai mua tại quầy
        this.customerUsername = order.getCustomer() != null
                ? order.getCustomer().getUsername() : "Khách lẻ";
        this.customerPhone    = order.getShippingAddress() != null
                ? order.getShippingAddress().getPhone() : "";
        this.orderDate        = order.getOrderDate();
        this.totalPrice       = order.getTotalPrice();
        this.status           = order.getStatus();
        this.trackingCode     = order.getTrackingCode();
        this.orderType        = order.getOrderType();
    }
}