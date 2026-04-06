package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderScheduler {

    private final OrderService orderService;
    private final AppOrderRepository orderRepository;

    public OrderScheduler(OrderService orderService, AppOrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    // Chạy mỗi giờ kiểm tra đơn DELIVERED quá 2 ngày
    @Scheduled(cron = "0 0 */2 * * *")
    public void autoConfirmOrders() {
        orderService.autoConfirmDeliveredOrders();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteCancelledOrders() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        orderRepository.findByStatus(OrderStatus.CANCELLED)
                .stream()
                .filter(o -> o.getOrderDate().isBefore(oneMonthAgo))
                .forEach(orderService::deleteCancelledOrder);
    }
}