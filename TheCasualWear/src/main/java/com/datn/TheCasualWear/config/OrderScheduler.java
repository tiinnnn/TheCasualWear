package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.NotificationRepository;
import com.datn.TheCasualWear.service.NotificationService;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    private final OrderService orderService;
    private final AppOrderRepository orderRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public OrderScheduler(OrderService orderService, AppOrderRepository orderRepository, NotificationService notificationService, NotificationRepository notificationRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    // Chạy mỗi 2 giờ kiểm tra đơn DELIVERED quá 2 ngày
    @Scheduled(cron = "0 0 */2 * * *")
    public void autoConfirmOrders() {
        orderService.autoConfirmDeliveredOrders();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteCancelledOrders() {
        orderService.deleteCancelledOrderAfterMonth();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteOldNotifications() {
        notificationRepository.deleteReadNotificationsOlderThan(
                LocalDateTime.now().minusDays(7));
    }
}