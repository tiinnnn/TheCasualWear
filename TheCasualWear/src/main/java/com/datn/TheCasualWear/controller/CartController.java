package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.CartItem;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.service.AppUserService;
import com.datn.TheCasualWear.service.CartService;
import com.datn.TheCasualWear.service.ProductSaleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService        cartService;
    private final AppUserService     appUserService;
    private final ProductSaleService productSaleService; // MỚI: badge/giá sale cho trang giỏ hàng

    private AppUser getCurrentUser(Authentication auth) {
        return appUserService.getUserByUsername(auth.getName());
    }

    @GetMapping
    public String viewCart(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        List<CartItem> cartItems = cartService.getCartItems(user);

        // Giá đã áp sale cho từng dòng, khóa theo cartItemId — để tổng các
        // dòng luôn khớp với totalPrice (CartService.getTotalPrice cũng
        // tính theo giá đã áp sale).
        Map<Integer, BigDecimal> itemPrices = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            itemPrices.put(item.getId(), cartService.getEffectiveUnitPrice(item));
        }

        // Sale đang chạy theo productId — để hiện badge "-X%" + giá gốc gạch ngang
        List<Integer> productIds = cartItems.stream()
                .map(i -> i.getVariant().getProduct().getId())
                .distinct().toList();
        Map<Integer, ProductSale> activeSales = productSaleService.getActiveSalesByProductIds(productIds);

        model.addAttribute("cartItems",  cartItems);
        model.addAttribute("itemPrices", itemPrices);
        model.addAttribute("activeSales", activeSales);
        model.addAttribute("totalPrice", cartService.getTotalPrice(user));
        model.addAttribute("view", "shop/cart");
        return "layouts/shop-layout";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Integer variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            Authentication auth,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        String referer = request.getHeader("Referer");
        String redirectTo = (referer != null && !referer.isBlank()) ? referer : "/shop";

        try {
            cartService.addToCart(user, variantId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng!");
            redirectAttributes.addFlashAttribute("cartLink", true);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + redirectTo;
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Integer cartItemId,
                                 @RequestParam Integer quantity,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        try {
            cartService.updateQuantity(user, cartItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
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