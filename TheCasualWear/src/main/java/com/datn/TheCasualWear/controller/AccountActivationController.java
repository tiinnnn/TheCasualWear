package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.service.CashierAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Trang public (không cần đăng nhập) để khách bấm link trong email và đặt
 * mật khẩu cho tài khoản do cashier tạo. Cần thêm "/activate-account" và
 * "/activate-account/**" vào permitAll() trong SecurityConfig.
 */
@Controller
@RequiredArgsConstructor
public class AccountActivationController {

    private final CashierAccountService cashierAccountService;

    @GetMapping("/activate-account")
    public String showForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "shop/activate-account"; // templates/activate-account.html
    }

    @PostMapping("/activate-account")
    public String activate(@RequestParam String token,
                            @RequestParam String password,
                            @RequestParam String confirmPassword,
                            Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "shop/activate-account";
        }
        try {
            cashierAccountService.activateAccount(token, password);
            return "redirect:/auth/login?activated=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "shop/activate-account";
        }
    }
}
