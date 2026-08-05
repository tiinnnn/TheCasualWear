package com.datn.TheCasualWear.enums;

/**
 * Lý do hủy / hoàn đơn — dùng chung cho AppOrder.cancelReason bất kể đơn
 * đang ở trạng thái CANCELLED hay RETURNED. Đây là dữ liệu phục vụ audit
 * và báo cáo, KHÔNG ảnh hưởng tới luồng nghiệp vụ (workflow vẫn do
 * OrderStatus quyết định).
 *
 * Không phải reason nào cũng dùng ở mọi nơi:
 *   - CUSTOMER_REQUEST, PAYMENT_ISSUE, DUPLICATE_ORDER, OTHER: dùng chung.
 *   - OUT_OF_STOCK, DELIVERY_FAILED: chủ yếu admin hủy đơn online.
 *   - DEFECTIVE_PRODUCT, WRONG_ITEM: chủ yếu khi hoàn hàng (RETURNED).
 */
public enum CancelReason {
    CUSTOMER_REQUEST,   // khách đổi ý / không muốn mua nữa
    OUT_OF_STOCK,       // hết hàng, không đủ hàng để giao
    PAYMENT_ISSUE,      // vấn đề thanh toán (COD từ chối nhận, VNPay lỗi...)
    DELIVERY_FAILED,    // giao hàng thất bại (không liên lạc được, khách từ chối nhận...)
    DEFECTIVE_PRODUCT,  // sản phẩm lỗi/hỏng
    WRONG_ITEM,         // giao sai sản phẩm
    DUPLICATE_ORDER,    // đơn trùng / tạo nhầm (thường ở POS)
    OTHER               // khác — bắt buộc kèm cancelNote
}