package com.datn.TheCasualWear.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GuestCheckoutFormDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    private String street;

    @NotBlank(message = "Vui lòng nhập thành phố/tỉnh")
    private String city;

    private String district;

    // MỚI (4.5): mã GHN của địa chỉ đang nhập — lấy từ dropdown Quận/Huyện
    // + Phường/Xã theo GHN (ghn-address-cascade.js), KHÔNG phải cascade
    // 2 cấp city/district ở trên (khác hệ mã, xem GhnService). NULL nếu
    // GHN lỗi lúc load dropdown hoặc khách bỏ qua — OrderService fallback
    // về phí region-based khi thiếu.
    private Integer ghnProvinceId;
    private Integer ghnDistrictId;
    private String ghnWardCode;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    private String voucherCode;
}