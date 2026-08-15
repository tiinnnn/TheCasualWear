package com.datn.TheCasualWear.pos;

import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.service.StockMovementLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Tự động hoàn kho các giỏ POS "treo" quá TIMEOUT_MINUTES phút không có
 * hoạt động (không add/sửa/xóa item) — ví dụ cashier bỏ ca không thanh
 * toán, hoặc quên đóng tab giỏ. @EnableScheduling đã bật sẵn cho
 * SaleScheduler nên không cần khai báo lại ở đây.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PosCartTimeoutScheduler {

    private static final int TIMEOUT_MINUTES = 30;

    private final PosCartRegistry cartRegistry;
    private final StockMovementLogService stockMovementLogService;

    // Quét mỗi 5 phút — đủ gần để giỏ treo không giữ kho oan quá lâu,
    // không cần chạy dày hơn vì mốc timeout là 30 phút.
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void releaseExpiredCarts() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        for (PosCart cart : cartRegistry.getAll()) {
            if (cart.getLastActivityAt().isBefore(threshold)) {
                releaseCart(cart);
                cartRegistry.remove(cart.getCartId());
            }
        }
    }

    private void releaseCart(PosCart cart) {
        String reason = "Giỏ treo quá " + TIMEOUT_MINUTES + " phút không hoạt động — tự động hoàn kho";
        for (CounterCartItemDTO item : cart.getItemList()) {
            try {
                // actor = null: job hệ thống chạy nền, không gắn với cashier nào
                stockMovementLogService.releaseForPosCart(
                        item.getVariantId(), item.getQuantity(), cart.getCartId(), reason, null);
            } catch (Exception e) {
                log.error("Lỗi hoàn kho timeout — giỏ POS #{}, variant #{}: {}",
                        cart.getCartId(), item.getVariantId(), e.getMessage());
            }
        }
    }
}
