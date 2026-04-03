package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Address;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AddressService;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AppUserService appUserService;
    private final AddressService addressService;
    private final OrderService orderService;

    public AccountController(AppUserService appUserService,
                             AddressService addressService,
                             OrderService orderService) {
        this.appUserService = appUserService;
        this.addressService = addressService;
        this.orderService = orderService;
    }

    private AppUser getCurrentUser(Authentication auth) {
        return appUserService.getUserByUsername(auth.getName());
    }

    // ==================== TRANG CHỦ TÀI KHOẢN ====================

    @GetMapping
    public String accountPage(Authentication auth, Model model) {
        model.addAttribute("user", getCurrentUser(auth));
        model.addAttribute("view", "shop/account/profile");
        return "layouts/shop-layout";
    }

    // ==================== CẬP NHẬT PROFILE ====================

    @PostMapping("/update")
    public String updateProfile(Authentication auth,
                                @ModelAttribute AppUser details,
                                RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        appUserService.updateProfile(user.getUsername(), details);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        return "redirect:/account";
    }

    // ==================== ĐỔI MẬT KHẨU ====================

    @GetMapping("/change-password")
    public String changePasswordPage(Authentication auth, Model model) {
        model.addAttribute("view", "shop/account/change-password");
        return "layouts/shop-layout";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication auth,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới không khớp!");
            return "redirect:/account/change-password";
        }

        AppUser user = getCurrentUser(auth);
        appUserService.changePassword(user.getUsername(), oldPassword, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/account";
    }

    // ==================== ĐỊA CHỈ ====================

    @GetMapping("/address")
    public String addressPage(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        model.addAttribute("view", "shop/account/address");
        return "layouts/shop-layout";
    }

    @GetMapping("/address/add")
    public String addAddressPage(Model model,Authentication auth) {
        AppUser user = getCurrentUser(auth);
        Address address = new Address();
        address.setFullName(user.getUsername());
        address.setPhone(user.getPhone());
        model.addAttribute("address", address);
        model.addAttribute("view", "shop/account/address-form");
        model.addAttribute("view", "shop/account/address-form");
        return "layouts/shop-layout";
    }

    @PostMapping("/address/add")
    public String addAddress(Authentication auth,
                             @ModelAttribute Address address,
                             RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        addressService.addAddress(user, address);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ thành công!");
        return "redirect:/account/address";
    }

    @GetMapping("/address/edit/{id}")
    public String editAddressPage(@PathVariable Integer id,
                                  Authentication auth,
                                  Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("address", addressService.getAddressById(id, user));
        model.addAttribute("view", "shop/account/address-form");
        return "layouts/shop-layout";
    }

    @PostMapping("/address/edit/{id}")
    public String editAddress(@PathVariable Integer id,
                              Authentication auth,
                              @ModelAttribute Address details,
                              RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        addressService.updateAddress(id, user, details);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật địa chỉ thành công!");
        return "redirect:/account/address";
    }

    @GetMapping("/address/delete/{id}")
    public String deleteAddress(@PathVariable Integer id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        addressService.deleteAddress(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa địa chỉ thành công!");
        return "redirect:/account/address";
    }

    @GetMapping("/address/set-default/{id}")
    public String setDefaultAddress(@PathVariable Integer id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        addressService.setDefaultAddress(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đặt địa chỉ mặc định!");
        return "redirect:/account/address";
    }

    // ==================== LỊCH SỬ ĐƠN HÀNG ====================

    @GetMapping("/orders")
    public String orderHistory(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("orders", orderService.getOrdersByUser(user));
        model.addAttribute("view", "shop/account/orders");
        return "layouts/shop-layout";
    }
}