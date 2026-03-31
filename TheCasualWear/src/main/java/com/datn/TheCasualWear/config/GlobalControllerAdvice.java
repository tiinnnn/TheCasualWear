package com.datn.TheCasualWear.config;

import com.datn.TheCasualWear.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CategoryService categoryService;

    public GlobalControllerAdvice(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Tự động truyền categories vào tất cả trang
    @ModelAttribute("navCategories")
    public java.util.List<?> navCategories() {
        return categoryService.getAllCategories();
    }
}