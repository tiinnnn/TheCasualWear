package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder){
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        dataBinder.registerCustomEditor(String.class,stringTrimmerEditor);
    }

    @GetMapping
    public String showProductPage(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("product", new Product()); // form mặc định là tạo mới
        return "admin/product";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("product", productService.getProductById(id)); // load vào form
        return "admin/product";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productService.getAllProducts());
            return "admin/product";
        }

        if (product.getId() == null) {
            productService.createProduct(product); // Create
        } else {
            productService.updateProduct(product.getId(), product); // Update
        }

        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return "redirect:/admin/products";
    }
}


