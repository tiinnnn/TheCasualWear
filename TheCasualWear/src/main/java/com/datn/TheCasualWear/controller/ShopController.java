package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.service.CategoryService;
import com.datn.TheCasualWear.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ShopController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ShopController(ProductService productService,
                          CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    //trang chu
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
        Page<Product> productPage = productService.getShopProducts(
                keyword, sort, category, page);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("view", "shop/shop");
        return "layouts/shop-layout";
    }

    // Trang chủ - related products trong productDetail
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("variants", productService.getProductVariants(id));

        // Sửa lại - thêm null và 0
        if (product.getCategory() != null) {
            model.addAttribute("relatedProducts",
                    productService.getShopProducts(null, "newest",
                                    product.getCategory().getId(), 0)
                            .getContent()  // ← thêm getContent() vì giờ trả về Page
                            .stream()
                            .filter(p -> !p.getId().equals(id))
                            .limit(4)
                            .toList()
            );
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
