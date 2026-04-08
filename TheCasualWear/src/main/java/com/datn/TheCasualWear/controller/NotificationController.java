package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserService appUserService;

    public NotificationController(NotificationService notificationService,
                                  AppUserService appUserService) {
        this.notificationService = notificationService;
        this.appUserService = appUserService;
    }

    // Đánh dấu đã đọc tất cả và redirect
    @GetMapping("/read-all")
    public String readAll(Authentication auth) {
        AppUser user = appUserService.getUserByUsername(auth.getName());
        notificationService.markAllRead(user.getId());
        return "redirect:/account/orders";
    }
}