package com.datn.TheCasualWear.pos;

import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Giỏ hàng tạm của POS — sống trong {@link PosCartRegistry} (in-memory),
 * KHÔNG phải AppOrder trong DB. Chỉ trở thành AppOrder thật khi cashier
 * bấm "Thanh toán" thành công (xem CashierService.checkout()).
 *
 * Stock của từng item đã bị trừ (giữ chỗ) ngay khi thêm vào giỏ — xem
 * StockMovementLogService.reserveForPosCart()/releaseForPosCart().
 */
@Getter
@Setter
public class PosCart {

    private String cartId;
    private String label;
    private Integer cashierId;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    // key = variantId, để tra/cộng dồn số lượng nhanh khi add trùng sản phẩm
    private final Map<Integer, CounterCartItemDTO> items = new LinkedHashMap<>();

    /** Cập nhật lại mốc "hoạt động gần nhất" — dùng để @Scheduled xét timeout. */
    public void touch() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CounterCartItemDTO> getItemList() {
        return new ArrayList<>(items.values());
    }

    public BigDecimal getGrandTotal() {
        return items.values().stream()
                .map(CounterCartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
