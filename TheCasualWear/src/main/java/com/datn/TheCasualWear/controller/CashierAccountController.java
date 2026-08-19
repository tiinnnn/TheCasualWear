package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.service.CashierAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint JSON để modal "Tạo tài khoản cho khách" trên Pos.html gọi bằng
 * fetch (POST, query params — cùng pattern với /cashier/checkout/vnpay/init
 * đã có sẵn trong Pos.html). Route nằm dưới /cashier/** nên đã tự động được
 * bảo vệ bởi SecurityConfig (chỉ CASHIER/OWNER/ADMIN).
 */
@RestController
@RequestMapping("/cashier/accounts")
@RequiredArgsConstructor
public class CashierAccountController {

    private final CashierAccountService cashierAccountService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestParam String email,
                                     @RequestParam(required = false) String phone) {
        try {
            var result = cashierAccountService.getOrCreateAccountForCustomer(email, phone);
            String message = result.newlyCreated()
                    ? "Đã tạo tài khoản, email kích hoạt đã được gửi tới " + email + "."
                    : "Email này đã có tài khoản sẵn — đơn hàng sẽ được gắn vào tài khoản đó.";

            return ResponseEntity.ok(Map.of(
                    "customerId", result.user().getId(),
                    "username", result.user().getUsername(),
                    "phone", result.user().getPhone() != null ? result.user().getPhone() : "",
                    "newlyCreated", result.newlyCreated(),
                    "message", message
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
