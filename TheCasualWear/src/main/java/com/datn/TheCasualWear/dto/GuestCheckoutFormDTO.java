package com.datn.TheCasualWear.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Form checkout khách vãng lai (4.1) — nhập trực tiếp thông tin giao hàng +
 * liên hệ, KHÔNG có danh sách địa chỉ để chọn (khác checkout.html của user
 * đã đăng nhập, nơi addresses lấy từ addressService.getAddressesByUser).
 * Dùng cho GET/POST /order/checkout-guest.
 *
 * MỚI: implements Serializable — object này được lưu thẳng vào HttpSession
 * (session.setAttribute("pendingGuestCheckoutForm", form)) trong luồng VNPay
 * guest, cần khi session bị serialize (Tomcat persist session, Redis session...).
 */
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

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    private String voucherCode;
}