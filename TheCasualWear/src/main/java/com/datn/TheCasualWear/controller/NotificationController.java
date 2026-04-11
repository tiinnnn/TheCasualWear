package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.Notification;
import com.datn.TheCasualWear.repository.NotificationRepository;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserService appUserService;
    private final NotificationRepository notificationRepository;

    // Đánh dấu đã đọc tất cả và redirect
    @GetMapping("/read-all")
    public String readAll(Authentication auth, HttpServletRequest request) {
        AppUser user = appUserService.getUserByUsername(auth.getName());
        notificationService.markAllRead(user.getId());

        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
    // Đánh dấu đã đọc 1 thông báo và redirect
    @GetMapping("/read/{id}")
    public String readOne(@PathVariable Integer id, Authentication auth) {
        Notification n = notificationRepository.findById(id).orElse(null);

        if (n != null) {
            // Kiểm tra thông báo có thuộc user đang đăng nhập không
            AppUser user = appUserService.getUserByUsername(auth.getName());
            if (n.getUser().getId().equals(user.getId())) {
                notificationService.markRead(id);
            }
            String link = (n.getLink() != null) ? n.getLink() : "/account/orders";
            return "redirect:" + link;
        }

        return "redirect:/";
    }
//    // Đánh dấu đã đọc 1 thông báo và redirect
//    @GetMapping("/read-notification")
//    public String readNotification(Authentication auth, HttpServletRequest request) {
//        AppUser user = appUserService.getUserByUsername(auth.getName());
//        notificationService.markRead(user.getId());
//
//        // Nếu user là admin thì luôn redirect về /admin
//        if ("ROLE_ADMIN".equals(user.getRoles())) {
//            return "redirect:/admin";
//        }
//        // Nếu không phải admin, thì check referer
//        String referer = request.getHeader("Referer");
//        if (referer != null && referer.contains("/admin")) {
//            return "redirect:/admin";
//        }
//        return "redirect:/account/orders";
//    }
}