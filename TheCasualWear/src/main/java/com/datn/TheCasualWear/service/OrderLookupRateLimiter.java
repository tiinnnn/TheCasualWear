package com.datn.TheCasualWear.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chống brute-force cho tra cứu đơn hàng khách vãng lai (4.2/6.6) — khoá tạm
 * theo IP sau N lần thử sai liên tiếp. Đơn giản (ConcurrentHashMap trong bộ
 * nhớ), không cần thêm dependency (Bucket4j...) vì quy mô DATN không cần.
 *
 * ⚠️ Lưu ý khi triển khai thật: dữ liệu mất khi restart app, và không dùng
 * được nếu chạy nhiều instance (cần Redis khi đó) — chấp nhận được cho demo.
 */
@Component
public class OrderLookupRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 5 * 60 * 1000; // 5 phút

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public String keyFor(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public boolean isBlocked(String key) {
        Attempt a = attempts.get(key);
        if (a == null) return false;

        boolean expired = System.currentTimeMillis() - a.firstFailureAt >= BLOCK_DURATION_MS;
        if (expired) {
            attempts.remove(key);
            return false;
        }
        return a.count.get() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, a) -> {
            if (a == null || System.currentTimeMillis() - a.firstFailureAt >= BLOCK_DURATION_MS) {
                return new Attempt(System.currentTimeMillis());
            }
            a.count.incrementAndGet();
            return a;
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private static class Attempt {
        final AtomicInteger count = new AtomicInteger(1);
        final long firstFailureAt;

        Attempt(long firstFailureAt) {
            this.firstFailureAt = firstFailureAt;
        }
    }
}
