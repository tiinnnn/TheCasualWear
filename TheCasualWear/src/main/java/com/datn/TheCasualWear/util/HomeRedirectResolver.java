package com.datn.TheCasualWear.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xác định trang "home" phù hợp theo vai trò CAO NHẤT của user.
 * Ưu tiên: ADMIN (gồm OWNER) > CASHIER > CUSTOMER.
 * Chưa đăng nhập / anonymous -> vẫn là "/" (trang shop công khai).
 */
public final class HomeRedirectResolver {

    private HomeRedirectResolver() {}

    public static String resolveHomeRedirect(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/";
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // dạng "ROLE_ADMIN"
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OWNER")) {
            return "redirect:/admin";
        }
        if (roles.contains("ROLE_CASHIER")) {
            return "redirect:/cashier";
        }
        return "redirect:/"; // CUSTOMER
    }
}