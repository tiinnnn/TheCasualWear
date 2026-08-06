package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductSale;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.entity.VariantImage;
import com.datn.TheCasualWear.repository.CategoryRepository;
import com.datn.TheCasualWear.service.CollectionService;
import com.datn.TheCasualWear.service.ProductSaleService;
import com.datn.TheCasualWear.service.ProductService;
import com.datn.TheCasualWear.service.ProductVariantService;
import com.datn.TheCasualWear.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService        productService;
    private final ProductVariantService variantService;
    private final WishlistService       wishlistService;
    private final CollectionService     collectionService;
    private final CategoryRepository    categoryRepository;
    private final ProductSaleService    productSaleService; // MỚI: giá/badge sale cho trang shop

    @GetMapping("/")
    public String homePage(Model model) {
        // San pham moi nhat
        List<Product> newestProducts = productService.getNewestProducts();
        model.addAttribute("newestProducts", newestProducts);

        // Collections dang active
        model.addAttribute("collections", collectionService.getActiveCollections());

        // San pham theo tung danh muc (moi danh muc lay toi da 8 san pham)
        var categories = categoryRepository.findAll();
        var productsByCategory = new java.util.LinkedHashMap<String, java.util.List<Product>>();
        for (var cat : categories) {
            var products = productService
                    .getShopProducts(null, "newest", cat.getId(), 0)
                    .getContent().stream().limit(8).toList();
            if (!products.isEmpty()) {
                productsByCategory.put(cat.getName(), products);
            }
        }
        model.addAttribute("productsByCategory", productsByCategory);

        // MỚI: gom id của TẤT CẢ sản phẩm đang hiển thị trên trang (mới nhất +
        // theo từng danh mục) để lấy sale đang chạy trong 1 lần query, tránh
        // N+1. Template dùng activeSales.get(product.id) để hiện badge/giá sale.
        List<Integer> allProductIds = new ArrayList<>();
        newestProducts.forEach(p -> allProductIds.add(p.getId()));
        productsByCategory.values().forEach(list ->
                list.forEach(p -> allProductIds.add(p.getId())));
        model.addAttribute("activeSales",
                productSaleService.getActiveSalesByProductIds(allProductIds));

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

        // MỚI: sale đang chạy cho các sản phẩm trong trang này
        List<Integer> productIds = productPage.getContent().stream().map(Product::getId).toList();
        model.addAttribute("activeSales", productSaleService.getActiveSalesByProductIds(productIds));

        model.addAttribute("view", "shop/shop");
        return "layouts/shop-layout";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Integer id,
                                Authentication auth,
                                Model model) {
        Product product = productService.getProductById(id);
        List<ProductVariant> variants = variantService.getVariantsByProduct(id);

        List<Map<String, Object>> variantData = new ArrayList<>();
        for (ProductVariant v : variants) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              v.getId());
            m.put("stock",           v.getStock());
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

        // MỚI: giá đã áp sale (nếu đang có sale chạy) + chính đợt sale đó,
        // để template hiển thị giá gạch ngang / badge "-X%" / đếm ngược.
        Optional<ProductSale> activeSale = productSaleService.getActiveSale(product);
        model.addAttribute("activeSale", activeSale.orElse(null));
        model.addAttribute("effectivePrice", productSaleService.getEffectivePrice(product));

        // Wishlist: check nếu user đã đăng nhập (loại trừ anonymousUser)
        boolean isWishlisted = auth != null
                && auth.isAuthenticated()
                && !auth.getName().equals("anonymousUser")
                && wishlistService.isWishlisted(auth.getName(), id);
        model.addAttribute("isWishlisted", isWishlisted);

        if (product.getCategory() != null) {
            List<Product> relatedProducts = productService.getShopProducts(null, "newest",
                            product.getCategory().getId(), 0)
                    .getContent().stream()
                    .filter(p -> !p.getId().equals(id))
                    .limit(4).toList();
            model.addAttribute("relatedProducts", relatedProducts);

            // Sale đang chạy cho các sản phẩm liên quan
            List<Integer> relatedIds = relatedProducts.stream().map(Product::getId).toList();
            model.addAttribute("relatedActiveSales",
                    productSaleService.getActiveSalesByProductIds(relatedIds));
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