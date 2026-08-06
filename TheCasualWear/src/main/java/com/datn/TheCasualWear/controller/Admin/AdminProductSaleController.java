package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.service.ProductSaleService;
import com.datn.TheCasualWear.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/products/{productId}/sales")
@RequiredArgsConstructor
public class AdminProductSaleController {

    private final ProductSaleService saleService;
    private final ProductService     productService;

    // TRANG QUẢN LÝ SALE CỦA 1 SẢN PHẨM
    // (template sẽ làm ở bước sau — admin/product/sales.html,
    // theo cùng pattern với admin/product/variants.html)
    @GetMapping
    public String salePage(@PathVariable Integer productId, Model model) {
        Product product = productService.getProductById(productId);
        List<ProductSale> sales = saleService.getSalesByProduct(productId);
        model.addAttribute("product", product);
        model.addAttribute("sales", sales);
        model.addAttribute("view", "admin/product/sales");
        return "layouts/admin-layout";
    }

    @PostMapping("/add")
    public String addSale(
            @PathVariable Integer productId,
            @RequestParam BigDecimal discountPercent,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            RedirectAttributes ra) {
        try {
            Product product = productService.getProductById(productId);
            saleService.createSale(product, discountPercent, startDate, endDate);
            ra.addFlashAttribute("successMessage", "Đã tạo đợt sale cho sản phẩm!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/sales";
    }

    @PostMapping("/edit/{saleId}")
    public String editSale(
            @PathVariable Integer productId,
            @PathVariable Integer saleId,
            @RequestParam BigDecimal discountPercent,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            RedirectAttributes ra) {
        try {
            saleService.updateSale(saleId, discountPercent, startDate, endDate);
            ra.addFlashAttribute("successMessage", "Đã cập nhật đợt sale!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/sales";
    }

    @GetMapping("/deactivate/{saleId}")
    public String deactivateSale(@PathVariable Integer productId,
                                 @PathVariable Integer saleId,
                                 RedirectAttributes ra) {
        saleService.deactivateSale(saleId);
        ra.addFlashAttribute("successMessage", "Đã tắt đợt sale!");
        return "redirect:/admin/products/" + productId + "/sales";
    }

    @GetMapping("/delete/{saleId}")
    public String deleteSale(@PathVariable Integer productId,
                             @PathVariable Integer saleId,
                             RedirectAttributes ra) {
        saleService.deleteSale(saleId);
        ra.addFlashAttribute("successMessage", "Đã xóa đợt sale!");
        return "redirect:/admin/products/" + productId + "/sales";
    }
}