package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    //FORM NHẬP EMAIL

    @GetMapping
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping
    public String handleForgotPassword(@RequestParam String email,
                                       RedirectAttributes redirectAttributes) {
        passwordResetService.sendResetEmail(email);
        // Luôn hiển thị thông báo thành công dù email có tồn tại hay không
        redirectAttributes.addFlashAttribute("successMessage",
                "Nếu email tồn tại, bạn sẽ nhận được link đặt lại mật khẩu trong vài phút.");
        return "redirect:/forgot-password";
    }

    //FORM ĐẶT LẠI MẬT KHẨU

    @GetMapping("/reset")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset")
    public String handleResetPassword(@RequestParam String token,
                                      @RequestParam String newPassword,
                                      RedirectAttributes redirectAttributes) {
        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (!success) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Link đã hết hạn hoặc không hợp lệ. Vui lòng thử lại.");
            return "redirect:/forgot-password";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
    }
}
