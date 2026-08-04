package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Shift;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.CartService;
import com.datn.TheCasualWear.service.CategoryService;
import com.datn.TheCasualWear.service.NotificationService;
import com.datn.TheCasualWear.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CategoryService categoryService;
    private final AppUserService appUserService;
    private final NotificationService notificationService;
    private final CartService cartService;
    private final AppUserRepository appUserRepository;
    private final ShiftService shiftService;

    // Tự động truyền categories vào tất cả trang
    @ModelAttribute("navCategories")
    public java.util.List<?> navCategories() {
        return categoryService.getAllCategories();
    }

    @ModelAttribute("unreadCount")
    public int unreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            return 0;
        }
        try {
            AppUser user = appUserService.getUserByUsername(authentication.getName());
            return notificationService.countUnread(user.getId());
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("userNotifications")
    public List<?> userNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            return List.of();
        }
        try {
            AppUser user = appUserService.getUserByUsername(authentication.getName());
            return notificationService.getUserNotifications(user.getId());
        } catch (Exception e) {
            return List.of();
        }
    }

    @ModelAttribute("cartCount")
    public int cartCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return 0;
        try {
            AppUser user = appUserRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            if (user == null) return 0;
            return cartService.getCartItemCount(user);
        } catch (Exception e) {
            return 0;
        }
    }

    // Ca đang mở của cashier hiện tại (null nếu chưa mở ca hoặc không phải cashier).
    // Dùng để hiển thị nút "Đóng ca" + giờ mở ca trên navbar của cashier-layout.html.
    @ModelAttribute("currentShift")
    public Shift currentShift(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            return null;
        }
        try {
            AppUser user = appUserService.getUserByUsername(authentication.getName());
            return shiftService.getOpenShift(user).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}