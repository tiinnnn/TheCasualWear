package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.ShiftService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Chặn mọi request vào /cashier/** (trang POS) nếu cashier chưa mở ca —
// tự động redirect sang form mở ca. Đường dẫn /cashier/shift/** được loại
// trừ (đăng ký ở WebMvcConfig) để tránh redirect loop.
@Component
@RequiredArgsConstructor
public class ShiftAccessInterceptor implements HandlerInterceptor {

    private final ShiftService      shiftService;
    private final AppUserRepository appUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            return true; // chưa đăng nhập → để Spring Security xử lý (redirect login)
        }

        AppUser cashier = appUserRepository
                .findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElse(null);
        if (cashier == null) {
            return true;
        }

        if (!shiftService.hasOpenShift(cashier)) {
            response.sendRedirect(request.getContextPath() + "/cashier/shift/open");
            return false;
        }
        return true;
    }
}
