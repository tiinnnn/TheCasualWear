package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.AppUser;

/**
 * Khách hàng có nhiều đơn CANCELLED/RETURNED — chỉ để admin xem xét thủ công,
 * KHÔNG tự động khóa/chặn tài khoản. Nhiều khách hủy vì lý do chính đáng
 * (hết hàng, giao trễ...) nên việc "blacklist" cần con người quyết định.
 */
public record FrequentCancellerDTO(AppUser customer, long cancelledCount,
                                    long returnedCount, long totalCount) {
}
