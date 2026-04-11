package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.repository.NotificationRepository;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.service.NotificationService;
import com.datn.TheCasualWear.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    // Chạy mỗi 2 giờ kiểm tra đơn DELIVERED quá 2 ngày
    @Scheduled(cron = "0 0 */2 * * *")
    public void autoConfirmOrders() {
        orderService.autoConfirmDeliveredOrders();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteCancelledOrders() {
        orderService.deleteCancelledOrderAfterMonth();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void deleteOldNotifications() {
        notificationRepository.deleteReadNotificationsOlderThan(
                LocalDateTime.now().minusDays(3));
    }

    // Thông báo stock thấp — 3 ngày lần 8h sáng
    @Scheduled(cron = "0 0 8 */3 * ?")
    public void notifyLowStock() {
        List<Product> lowStockProducts = productRepository
                .findByIsDeletedFalseAndStockLessThanAndStockGreaterThan(5, 0);

        if (lowStockProducts.isEmpty()) return;

        StringBuilder msg = new StringBuilder("⚠️ Cảnh báo tồn kho thấp: ");
        lowStockProducts.forEach(p ->
                msg.append(p.getName())
                        .append(" (còn ").append(p.getStock()).append("), ")
        );

        // Bỏ dấu phẩy cuối
        String message = msg.toString().replaceAll(", $", "");

        notificationService.createNotificationForAdmins(message, "/admin/products");
    }

    // Thông báo sản phẩm hết hàng — 3 ngày lần 8h sáng
    @Scheduled(cron = "0 0 8 */3 * ?")
    public void notifyOutOfStock() {
        List<Product> outOfStockProducts = productRepository
                .findByIsDeletedFalseAndStock(0);

        if (outOfStockProducts.isEmpty()) return;

        StringBuilder msg = new StringBuilder("🚫 Sản phẩm hết hàng: ");
        outOfStockProducts.forEach(p ->
                msg.append(p.getName()).append(", ")
        );

        String message = msg.toString().replaceAll(", $", "");
        notificationService.createNotificationForAdmins(message, "/admin/products");
    }
}