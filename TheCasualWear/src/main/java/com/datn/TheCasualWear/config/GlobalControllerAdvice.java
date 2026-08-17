package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.CartService;
import com.datn.TheCasualWear.service.CategoryService;
import com.datn.TheCasualWear.service.GuestCartService;
import com.datn.TheCasualWear.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final GuestCartService guestCartService; // MỚI: badge giỏ hàng cho khách vãng lai

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

    // MỚI: giỏ hàng khách vãng lai, tương đương cartCount ở trên nhưng đọc từ
    // HttpSession thay vì DB (xem GuestCartService.getItemCount() — cố ý
    // không dùng getCart(session) để tránh tự tạo session rỗng cho mọi khách
    // ghé trang, vì @ModelAttribute này chạy trên MỌI request).
    @ModelAttribute("guestCartCount")
    public int guestCartCount(@AuthenticationPrincipal UserDetails userDetails,
                              HttpServletRequest request) {
        if (userDetails != null) return 0; // đã login -> dùng cartCount ở trên
        try {
            return guestCartService.getItemCount(request.getSession(false));
        } catch (Exception e) {
            return 0;
        }
    }
}