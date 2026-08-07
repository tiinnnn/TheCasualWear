package com.datn.TheCasualWear.dto;

import com.datn.TheCasualWear.enums.CancelReason;

public record CancelReasonStatDTO(CancelReason reason, long count) {
}
