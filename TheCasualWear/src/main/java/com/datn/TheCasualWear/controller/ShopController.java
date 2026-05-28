package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.entity.VariantImage;
import com.datn.TheCasualWear.service.ProductService;
import com.datn.TheCasualWear.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService        productService;
    private final ProductVariantService variantService;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("newestProducts", productService.getNewestProducts());
        model.addAttribute("view", "shop/home");
        return "layouts/shop-layout";
    }

    @GetMapping("/shop")
    public String shopPage(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String sort,
                           @RequestParam(required = false) Integer category,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        Page<Product> productPage = productService.getShopProducts(keyword, sort, category, page);
        model.addAttribute("products",         productPage.getContent());
        model.addAttribute("keyword",          keyword);
        model.addAttribute("sort",             sort);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("currentPage",      page);
        model.addAttribute("totalPages",       productPage.getTotalPages());
        model.addAttribute("totalItems",       productPage.getTotalElements());
        model.addAttribute("view", "shop/shop");
        return "layouts/shop-layout";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        List<ProductVariant> variants = variantService.getVariantsByProduct(id);

        // Build plain DTO list để tránh Jackson lazy-load lỗi khi serialize sang JSON
        List<Map<String, Object>> variantData = new ArrayList<>();
        for (ProductVariant v : variants) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              v.getId());
            m.put("stock",           v.getStock());
            m.put("priceAdjustment", v.getPriceAdjustment() != null
                    ? v.getPriceAdjustment().doubleValue() : 0.0);
            m.put("sku",             v.getSku());

            // Color
            Map<String, Object> color = null;
            if (v.getColor() != null) {
                color = new LinkedHashMap<>();
                color.put("id",   v.getColor().getId());
                color.put("name", v.getColor().getName());
            }
            m.put("color", color);

            // Size
            Map<String, Object> size = null;
            if (v.getSize() != null) {
                size = new LinkedHashMap<>();
                size.put("id",   v.getSize().getId());
                size.put("name", v.getSize().getName());
            }
            m.put("size", size);

            // ✅ Ảnh riêng của variant — dùng cho slideshow khi chọn màu
            // Fallback: nếu variant không có ảnh riêng thì dùng ảnh product
            List<String> imageUrls = new ArrayList<>();
            if (v.getImages() != null && !v.getImages().isEmpty()) {
                for (VariantImage img : v.getImages()) {
                    imageUrls.add(img.getImageUrl());
                }
            } else {
                // Fallback về product images
                if (product.getImages() != null) {
                    product.getImages().forEach(img -> imageUrls.add(img.getImageUrl()));
                }
            }
            m.put("images", imageUrls);

            variantData.add(m);
        }

        model.addAttribute("product",     product);
        model.addAttribute("variantData", variantData); // dùng trong JS
        model.addAttribute("variants",    variants);    // dùng trong Thymeleaf

        if (product.getCategory() != null) {
            model.addAttribute("relatedProducts",
                    productService.getShopProducts(null, "newest",
                                    product.getCategory().getId(), 0)
                            .getContent().stream()
                            .filter(p -> !p.getId().equals(id))
                            .limit(4).toList());
        }

        model.addAttribute("view", "shop/product-detail");
        return "layouts/shop-layout";
    }

    @GetMapping("/lien-he")
    public String lienHe(Model model) {
        model.addAttribute("view", "shop/lien-he");
        return "layouts/shop-layout";
    }

    @GetMapping("/chinh-sach-doi-tra")
    public String chinhSachDoiTra(Model model) {
        model.addAttribute("view", "shop/chinh-sach-doi-tra");
        return "layouts/shop-layout";
    }
}