package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dọn dẹp các tài khoản do cashier tạo nhưng khách không bao giờ bấm link
 * kích hoạt (có thể do cashier nhập sai email). Chạy hàng ngày lúc 3h sáng.
 *
 * QUAN TRỌNG: không hard-delete vô điều kiện — nếu tài khoản đã có đơn hàng
 * gắn vào (AppOrder.customer_id), xóa sẽ vi phạm FK constraint. Với trường
 * hợp đó chỉ giải phóng email/token để chủ nhân thật đăng ký lại được, giữ
 * nguyên record để không phá dữ liệu đơn hàng.
 *
 * ⚠️ Cần @EnableScheduling ở đâu đó trong project (thường đặt ở class
 * Application chính hoặc 1 @Configuration riêng) — nếu chưa có, thêm vào.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupScheduler {

    private final AppUserRepository appUserRepository;
    private final AppOrderRepository appOrderRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredActivations() {
        List<AppUser> expired = appUserRepository
                .findByActivationTokenIsNotNullAndActivationExpiresAtBefore(LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        int freed = 0;
        int deleted = 0;

        for (AppUser user : expired) {
            boolean hasOrders = appOrderRepository.existsByCustomerId(user.getId());
            if (hasOrders) {
                user.setEmail(null);
                user.setActivationToken(null);
                user.setActivationExpiresAt(null);
                appUserRepository.save(user);
                freed++;
            } else {
                appUserRepository.delete(user);
                deleted++;
            }
        }

        log.info("AccountCleanupScheduler: giải phóng {} tài khoản (đã có đơn hàng), " +
                "xóa hẳn {} tài khoản (chưa có đơn hàng).", freed, deleted);
    }
}
