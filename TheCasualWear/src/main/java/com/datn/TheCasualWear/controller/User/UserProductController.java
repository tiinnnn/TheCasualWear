package com.datn.TheCasualWear.controller.User;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class UserProductController {
    private final ProductService productService;

    public UserProductController(ProductService productService) {
        this.productService = productService;
    }

    // Danh sách sản phẩm cho user
    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "user/product-list";
    }

    // Chi tiết sản phẩm
    @GetMapping("/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "user/product-detail";
    }
}
