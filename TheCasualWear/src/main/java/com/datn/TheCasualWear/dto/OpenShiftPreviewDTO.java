package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.entity.Shift;

import java.math.BigDecimal;

// Ca đang OPEN kèm số liệu tạm tính (chưa chốt) — hiển thị ở phần
// "Đang diễn ra" trong báo cáo tổng kết cuối ngày.
public record OpenShiftPreviewDTO(
        Shift shift,
        BigDecimal previewCash,
        int previewItems
) {}
