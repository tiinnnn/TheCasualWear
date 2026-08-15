package com.datn.TheCasualWear.pos;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nơi lưu thật sự các giỏ POS đang mở — KHÔNG dùng HttpSession, vì
 * @Scheduled job (PosCartTimeoutScheduler) chạy nền cần quét được toàn bộ
 * giỏ của mọi cashier để tự hoàn kho giỏ "treo" quá lâu, mà HttpSession
 * chỉ tồn tại trong context của 1 request/user cụ thể nên bean chạy nền
 * không thể duyệt qua được.
 *
 * HttpSession của cashier (xem CashierController) chỉ giữ danh sách
 * cartId thuộc về mình + cartId đang active — dữ liệu giỏ thật (items,
 * lastActivityAt...) nằm hết ở đây.
 */
@Component
public class PosCartRegistry {

    private final ConcurrentHashMap<String, PosCart> carts = new ConcurrentHashMap<>();

    public PosCart create(Integer cashierId, String label) {
        PosCart cart = new PosCart();
        cart.setCartId(UUID.randomUUID().toString());
        cart.setCashierId(cashierId);
        cart.setLabel(label);
        carts.put(cart.getCartId(), cart);
        return cart;
    }

    public PosCart get(String cartId) {
        return cartId == null ? null : carts.get(cartId);
    }

    public void remove(String cartId) {
        if (cartId != null) carts.remove(cartId);
    }

    /** Dùng cho @Scheduled job quét toàn bộ giỏ (mọi cashier) tìm giỏ treo quá lâu. */
    public Collection<PosCart> getAll() {
        return carts.values();
    }
}
