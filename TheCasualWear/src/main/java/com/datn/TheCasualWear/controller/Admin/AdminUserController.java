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

        boolean isOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
        model.addAttribute("isOwner", isOwner);

        model.addAttribute("view", "admin/user/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/{id}/lock")
    public String lockUser(@PathVariable Integer id, RedirectAttributes ra) {
        appUserService.lockUser(id);
        ra.addFlashAttribute("successMessage", "Đã khóa tài khoản!");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Integer id, RedirectAttributes ra) {
        appUserService.unlockUser(id);
        ra.addFlashAttribute("successMessage", "Đã mở khóa tài khoản!");
        return "redirect:/admin/users";
    }
}