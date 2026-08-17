package com.datn.TheCasualWear.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestOrderLookupDTO {

    @NotBlank(message = "Vui lòng nhập mã đơn hàng")
    private String orderCode;
}