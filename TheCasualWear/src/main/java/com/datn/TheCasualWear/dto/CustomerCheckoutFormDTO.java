package com.datn.TheCasualWear.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Form checkout cho khách ĐÃ ĐĂNG NHẬP — thay thế cách nhận thẳng
 * shippingAddressId/billingAddressId (Integer) trước đây bằng field địa chỉ
 * nhập trực tiếp (giống GuestCheckoutFormDTO), để khách vừa có thể chọn
 * nhanh từ sổ địa chỉ đã lưu, vừa có thể sửa/nhập địa chỉ khác ngay tại
 * trang checkout mà không cần qua /account/address/add.
 *
 * Cách phân biệt "dùng địa chỉ đã lưu, không sửa gì" và "nhập/sửa địa chỉ
 * mới" (xem checkoutAddress.js ở checkout.html):
 *  - useExistingAddressId != null → FE xác nhận khách chưa đụng vào field
 *    nào sau khi chọn địa chỉ có sẵn → BE dùng thẳng Address đó theo id,
 *    KHÔNG tạo Address mới, bỏ qua saveAsDefault.
 *  - useExistingAddressId == null → khách đã tự nhập hoặc sửa field →
 *    BE tạo Address mới từ fullName/phone/street/city/district
 *    (OrderController.resolveShippingAddress → AddressService.createAddressForOrder):
 *      + saveAsDefault = true  → lưu vào sổ địa chỉ (user = customer,
 *        set làm mặc định, bỏ mặc định cũ).
 *      + saveAsDefault = false → KHÔNG lưu vào sổ (user = null), chỉ dùng
 *        cho đơn hàng này (giống cách guest checkout tạo Address).
 *
 * implements Serializable vì object có thể cần lưu vào session cho các luồng
 * tương tự guest checkout sau này — hiện luồng VNPay của user chỉ lưu lại
 * addressId đã resolve xong (xem OrderController), không lưu thẳng DTO này.
 */
@Getter
@Setter
public class CustomerCheckoutFormDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Khác null nếu khách dùng nguyên 1 địa chỉ đã lưu, không sửa gì.
    private Integer useExistingAddressId;

    // Có tick "Đặt làm địa chỉ mặc định" không — chỉ có ý nghĩa khi
    // useExistingAddressId == null (đang nhập/sửa địa chỉ mới).
    private Boolean saveAsDefault = false;

    @NotBlank(message = "Vui lòng nhập họ tên")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    private String street;

    @NotBlank(message = "Vui lòng nhập thành phố/tỉnh")
    private String city;

    private String district;

    private String voucherCode;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;
}
