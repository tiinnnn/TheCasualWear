package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AppUserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserService appUserService;

    public AdminUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String roleName,
                            @RequestParam(defaultValue = "0") int page,
                            Authentication auth, Model model) {
        Page<AppUser> userPage = appUserService.getAllUsers(keyword, roleName, page);
        model.addAttribute("users",       userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  userPage.getTotalPages());
        model.addAttribute("totalItems",  userPage.getTotalElements());
        model.addAttribute("keyword",     keyword);
        model.addAttribute("selectedRole", roleName);
        model.addAttribute("allRoles",    appUserService.getAllRoles());

        model.addAttribute("isOwner", isOwner(auth));

        model.addAttribute("view", "admin/user/list");
        return "layouts/admin-layout";
    }

    // Trước đây model chỉ dùng "isOwner" để ẩn/hiện nút ở view — controller
    // không hề check quyền, nên 1 ADMIN vẫn gọi thẳng được URL này để khóa
    // tài khoản của ADMIN khác hoặc của OWNER. Helper này check quyền thật
    // sự ở tầng server, không phụ thuộc UI.
    private boolean canLockOrUnlock(Authentication auth, AppUser target) {
        boolean callerIsOwner = isOwner(auth);
        boolean targetIsAdminOrOwner = target.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN") || r.getName().equals("ROLE_OWNER"));
        return callerIsOwner || !targetIsAdminOrOwner;
    }

    private boolean isOwner(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
    }

    @GetMapping("/{id}/lock")
    public String lockUser(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
        AppUser target = appUserService.getUserById(id);

        // Không cho tự khóa tài khoản của chính mình — kể cả Owner — để
        // tránh tự đá mình ra khỏi hệ thống (accidental self-lockout).
        if (target.getUsername().equals(auth.getName())) {
            ra.addFlashAttribute("errorMessage", "Bạn không thể tự khóa tài khoản của chính mình!");
            return "redirect:/admin/users";
        }
        if (!canLockOrUnlock(auth, target)) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền khóa tài khoản này!");
            return "redirect:/admin/users";
        }
        appUserService.lockUser(id);
        ra.addFlashAttribute("successMessage", "Đã khóa tài khoản!");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
        AppUser target = appUserService.getUserById(id);
        if (!canLockOrUnlock(auth, target)) {
            ra.addFlashAttribute("errorMessage", "Bạn không có quyền mở khóa tài khoản này!");
            return "redirect:/admin/users";
        }
        appUserService.unlockUser(id);
        ra.addFlashAttribute("successMessage", "Đã mở khóa tài khoản!");
        return "redirect:/admin/users";
    }
}