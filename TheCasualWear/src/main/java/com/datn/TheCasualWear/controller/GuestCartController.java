package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.dto.GuestCartItem;
import com.datn.TheCasualWear.service.GuestCartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Giỏ hàng cho khách vãng lai — song song với CartController, KHÔNG sửa
 * CartController hiện có (vốn gắn với AppUser đã login). Dùng GuestCartService
 * (HttpSession) thay vì CartService (DB).
 *
 * Khác biệt so với CartController: dùng variantId làm khóa cập nhật/xóa thay
 * vì cartItemId, vì GuestCartItem không có id riêng (không persist DB) —
 * xem GuestCartService.updateQuantity()/removeItem().
 */
@Controller
@RequestMapping("/cart-guest")
@RequiredArgsConstructor
public class GuestCartController {

    private final GuestCartService guestCartService;

    @GetMapping
    public String viewCart(HttpServletRequest request, Model model) {
        List<GuestCartItem> cartItems = guestCartService.getCart(request.getSession());
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", guestCartService.getTotalPrice(request.getSession()));
        model.addAttribute("view", "shop/cart-guest");
        return "layouts/shop-layout";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Integer variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        String referer = request.getHeader("Referer");
        String redirectTo = (referer != null && !referer.isBlank()) ? referer : "/shop";

        try {
            guestCartService.addItem(request.getSession(), variantId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng!");
            redirectAttributes.addFlashAttribute("cartLink", true);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + redirectTo;
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Integer variantId,
                                 @RequestParam Integer quantity,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        guestCartService.updateQuantity(request.getSession(), variantId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng!");
        return "redirect:/cart-guest";
    }

    @GetMapping("/remove/{variantId}")
    public String removeItem(@PathVariable Integer variantId,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        guestCartService.removeItem(request.getSession(), variantId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
        return "redirect:/cart-guest";
    }

    @GetMapping("/clear")
    public String clearCart(HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        guestCartService.clearCart(request.getSession());
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa toàn bộ giỏ hàng!");
        return "redirect:/cart-guest";
    }
}
