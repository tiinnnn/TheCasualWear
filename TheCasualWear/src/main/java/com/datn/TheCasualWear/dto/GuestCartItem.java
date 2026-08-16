package com.datn.TheCasualWear.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Một dòng trong giỏ hàng khách vãng lai — lưu trong HttpSession qua
 * GuestCartService, KHÔNG phải @Entity (không persist DB cho tới khi
 * checkout thành công, giống tư duy PosCartRegistry nhưng đơn giản hơn:
 * không multi-cart, không reservation stock).
 *
 * implements Serializable là BẮT BUỘC: Tomcat serialize toàn bộ HttpSession
 * ra đĩa (SESSIONS.ser) khi tắt/restart (đặc biệt do Spring Boot DevTools
 * auto-restart lúc dev). Thiếu Serializable làm hỏng việc đọc lại TOÀN BỘ
 * session cũ lúc khởi động lại — không chỉ guestCart mà cả session của user
 * đã login cũng bị mất theo, không phải lỗi cục bộ chỉ ảnh hưởng guest cart.
 */
@Data
public class GuestCartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer variantId;
    private String productName;
    private String sizeName;
    private String colorName;
    private String imageUrl;
    private BigDecimal unitPrice; // giá thực khách trả (đã áp sale nếu có)
    private Integer quantity;

    // Thông tin sale để hiển thị — null nếu không có sale đang chạy.
    // Giống CounterCartItemDTO: originalPrice để hiện gạch ngang,
    // discountPercent để hiện badge. unitPrice luôn dùng để tính tiền.
    private BigDecimal originalPrice;
    private BigDecimal discountPercent;

    public boolean isOnSale() {
        return discountPercent != null;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}