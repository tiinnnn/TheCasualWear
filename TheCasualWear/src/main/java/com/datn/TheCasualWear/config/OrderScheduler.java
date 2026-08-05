package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.repository.NotificationRepository;
import com.datn.TheCasualWear.service.NotificationService;
import com.datn.TheCasualWear.service.OrderService;
import com.datn.TheCasualWear.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService          orderService;
    private final ProductVariantService variantService;
    private final NotificationRepository notificationRepository;
    private final NotificationService   notificationService;


    // Mỗi ngày 0h — xóa đơn CANCELLED quá 1 tháng
    @Scheduled(cron = "0 0 0 * * *")
    public void deleteCancelledOrders() {
        orderService.deleteCancelledOrderAfterMonth();
    }

    // Mỗi giờ — xóa notification đã đọc quá 3 ngày
    @Scheduled(cron = "0 0 * * * *")
    public void deleteOldNotifications() {
        notificationRepository.deleteReadNotificationsOlderThan(
                LocalDateTime.now().minusDays(3));
    }


    @Scheduled(cron = "0 0 8 */3 * ?")
    public void notifyLowStock() {
        List<ProductVariant> lowStock = variantService.getLowStockVariants();
        if (lowStock.isEmpty()) return;

        StringBuilder msg = new StringBuilder("⚠️ Tồn kho thấp: ");
        lowStock.forEach(v -> msg
                .append(v.getProduct().getName())
                .append(" [")
                .append(v.getSize()  != null ? v.getSize().getName()  : "?")
                .append("/")
                .append(v.getColor() != null ? v.getColor().getName() : "?")
                .append("] còn ").append(v.getStock()).append(", "));

        notificationService.createNotificationForAdmins(
                msg.toString().replaceAll(", $", ""),
                "/admin/products");
    }


     // 8h sáng mỗi 3 ngày — cảnh báo variant hết hàng (stock = 0).
    @Scheduled(cron = "0 0 8 */3 * ?")
    public void notifyOutOfStock() {
        List<ProductVariant> outOfStock = variantService.getOutOfStockVariants();
        if (outOfStock.isEmpty()) return;

        StringBuilder msg = new StringBuilder("🚫 Hết hàng: ");
        outOfStock.forEach(v -> msg
                .append(v.getProduct().getName())
                .append(" [")
                .append(v.getSize()  != null ? v.getSize().getName()  : "?")
                .append("/")
                .append(v.getColor() != null ? v.getColor().getName() : "?")
                .append("], "));

        notificationService.createNotificationForAdmins(
                msg.toString().replaceAll(", $", ""),
                "/admin/products");
    }
}