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
                            @RequestParam(defaultValue = "0") int page,
                            Authentication auth, Model model) {
        Page<AppUser> userPage = appUserService.getAllUsers(keyword, page);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("keyword", keyword);

        boolean isOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("view", "admin/user/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/{id}/lock")
    public String lockUser(@PathVariable Integer id,
                           RedirectAttributes redirectAttributes) {
        appUserService.lockUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản!");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes) {
        appUserService.unlockUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản!");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role/add")
    public String addRole(@PathVariable Integer id,
                          @RequestParam String roleName,
                          Authentication auth,
                          RedirectAttributes redirectAttributes) {
        // Chỉ OWNER mới được cấp ROLE_ADMIN và ROLE_OWNER
        boolean isOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));

        if (!isOwner && (roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_OWNER"))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn không có quyền cấp role này!");
            return "redirect:/admin/users";
        }

        appUserService.addRole(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm role!");
        return "redirect:/admin/users";
    }


    @PostMapping("/{id}/role/remove")
    public String removeRole(@PathVariable Integer id,
                             @RequestParam String roleName,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        boolean isOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));

        if (!isOwner && (roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_OWNER"))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Bạn không có quyền xóa role này!");
            return "redirect:/admin/users";
        }

        appUserService.removeRole(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa role!");
        return "redirect:/admin/users";
    }
}