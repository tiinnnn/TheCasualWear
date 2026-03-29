package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.service.CategoryService;
import com.datn.TheCasualWear.service.ProductService;
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
                           Model model) {
        model.addAttribute("products", productService.getShopProducts(keyword, sort));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("view", "shop/shop");
        return "layouts/shop-layout";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        if (product.getCategory() != null) {
            model.addAttribute("relatedProducts",
                    productService.getShopProducts(null, "newest")
                            .stream()
                            .filter(p -> p.getCategory() != null
                                    && p.getCategory().getId().equals(product.getCategory().getId())
                                    && !p.getId().equals(id))
                            .limit(4)
                            .toList());
        }
        model.addAttribute("view", "shop/product-detail");
        return "layouts/shop-layout";
    }
}