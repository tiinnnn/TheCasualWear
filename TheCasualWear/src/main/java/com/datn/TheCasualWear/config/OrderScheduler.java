package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderScheduler {

    private final OrderService orderService;

    public OrderScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    // Chạy mỗi giờ kiểm tra đơn DELIVERED quá 2 ngày
    @Scheduled(cron = "0 0 */2 * * *")
    public void autoConfirmOrders() {
        orderService.autoConfirmDeliveredOrders();
    }
}