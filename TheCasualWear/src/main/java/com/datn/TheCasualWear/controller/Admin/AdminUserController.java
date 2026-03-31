package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.service.AppUserService;
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
    public String listUsers(Model model) {
        model.addAttribute("users", appUserService.getAllUsers());
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
                          RedirectAttributes redirectAttributes) {
        appUserService.addRole(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm role!");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role/remove")
    public String removeRole(@PathVariable Integer id,
                             @RequestParam String roleName,
                             RedirectAttributes redirectAttributes) {
        appUserService.removeRole(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa role!");
        return "redirect:/admin/users";
    }
}