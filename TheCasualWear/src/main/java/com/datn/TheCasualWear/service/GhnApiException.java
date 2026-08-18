package com.datn.TheCasualWear.service;

/**
 * Ném ra khi gọi GHN API lỗi/timeout/response không hợp lệ. Nơi gọi
 * calculateFee() PHẢI bắt exception này và fallback về phí region-based —
 * không được để lỗi này làm hỏng luồng checkout (xem OrderService).
 */
public class GhnApiException extends RuntimeException {
    public GhnApiException(String message) {
        super(message);
    }

    public GhnApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
