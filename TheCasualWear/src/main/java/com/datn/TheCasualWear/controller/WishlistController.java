package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public String wishlistPage(Authentication auth, Model model) {
        model.addAttribute("wishlist", wishlistService.getWishlist(auth.getName()));
        model.addAttribute("view", "shop/account/wishlist");
        return "layouts/shop-layout";
    }

    @PostMapping("/toggle/{productId}")
    public String toggle(@PathVariable Integer productId,
                         @RequestParam(defaultValue = "/product/" + "") String redirectUrl,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        boolean wishlisted = wishlistService.toggle(auth.getName(), productId);
        return "redirect:/product/" + productId;
    }

    @PostMapping("/remove/{productId}")
    public String remove(@PathVariable Integer productId,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        wishlistService.remove(auth.getName(), productId);
        return "redirect:/wishlist";
    }
}