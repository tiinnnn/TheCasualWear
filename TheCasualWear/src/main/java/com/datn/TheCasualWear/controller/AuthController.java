package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AppUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }


    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !auth.getName().equals("anonymousUser")) {
            return "redirect:/";
        }
        if (error != null) model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không đúng!");
        if (logout != null) model.addAttribute("successMessage", "Đăng xuất thành công!");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !auth.getName().equals("anonymousUser")) {
            return "redirect:/";
        }
        model.addAttribute("user", new AppUser());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") AppUser user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        if (!user.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu nhập lại không khớp!");
            return "redirect:/auth/register";
        }
        appUserService.register(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
    }
}