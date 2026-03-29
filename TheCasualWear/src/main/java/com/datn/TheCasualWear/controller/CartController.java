package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final AppUserService appUserService;

    public CartController(CartService cartService, AppUserService appUserService) {
        this.cartService = cartService;
        this.appUserService = appUserService;
    }

    // Lấy user hiện tại đang đăng nhập
    private AppUser getCurrentUser(Authentication auth) {
        return appUserService.getUserByUsername(auth.getName());
    }

    @GetMapping
    public String viewCart(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("cartItems", cartService.getCartItems(user));
        model.addAttribute("totalPrice", cartService.getTotalPrice(user));
        model.addAttribute("view", "shop/cart");
        return "layouts/shop-layout";
    }
    //them vao sau moi product de add vao cart
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        cartService.addToCart(user, productId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng!");
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Integer cartItemId,
                                 @RequestParam Integer quantity,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        cartService.updateQuantity(user, cartItemId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng!");
        return "redirect:/cart";
    }

    @GetMapping("/remove/{cartItemId}")
    public String removeItem(@PathVariable Integer cartItemId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        cartService.removeItem(user, cartItemId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart(Authentication auth,
                            RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        cartService.clearCart(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa toàn bộ giỏ hàng!");
        return "redirect:/cart";
    }
}