package com.datn.TheCasualWear.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Dùng ở form tra cứu đơn hàng khách vãng lai (4.2/6.6)
@Getter
@Setter
public class GuestOrderLookupDTO {

    @NotBlank(message = "Vui lòng nhập mã đơn hàng")
    private String orderCode;

    // Chấp nhận SĐT hoặc email — validate ở service, không tách 2 field
    // riêng để tránh lộ qua UI việc hệ thống check field nào trước.
    @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email đã dùng khi đặt hàng")
    private String contact;
}
