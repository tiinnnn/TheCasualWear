package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Category;
import com.datn.TheCasualWear.entity.Color;
import com.datn.TheCasualWear.entity.Size;
import com.datn.TheCasualWear.service.CategoryService;
import com.datn.TheCasualWear.service.ColorService;
import com.datn.TheCasualWear.service.SizeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final SizeService sizeService;
    private final ColorService colorService;

    public AdminCategoryController(CategoryService categoryService,
                                   SizeService sizeService,
                                   ColorService colorService) {
        this.categoryService = categoryService;
        this.sizeService = sizeService;
        this.colorService = colorService;
    }

    // Danh sách category + size + color chung 1 trang
    @GetMapping
    public String listPage(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes", sizeService.getAllSizes());
        model.addAttribute("colors", colorService.getAllColors());
        model.addAttribute("newCategory", new Category());
        model.addAttribute("newSize", new Size());
        model.addAttribute("newColor", new Color());
        model.addAttribute("view", "admin/category/list");
        return "layouts/admin-layout";
    }

    @PostMapping("/add")
    public String addCategory(@ModelAttribute("newCategory") Category category,
                              RedirectAttributes redirectAttributes) {
        categoryService.createCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Integer id,
                               @ModelAttribute Category details,
                               RedirectAttributes redirectAttributes) {
        categoryService.updateCategory(id, details);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Integer id,
                                 RedirectAttributes redirectAttributes) {
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/size/add")
    public String addSize(@ModelAttribute("newSize") Size size,
                          RedirectAttributes redirectAttributes) {
        sizeService.createSize(size);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm size thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/size/edit/{id}")
    public String editSize(@PathVariable Integer id,
                           @ModelAttribute Size details,
                           RedirectAttributes redirectAttributes) {
        sizeService.updateSize(id, details);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật size thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/size/delete/{id}")
    public String deleteSize(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes) {
        sizeService.deleteSize(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa size thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/color/add")
    public String addColor(@ModelAttribute("newColor") Color color,
                           RedirectAttributes redirectAttributes) {
        colorService.createColor(color);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm màu thành công!");
        return "redirect:/admin/categories";
    }

    @PostMapping("/color/edit/{id}")
    public String editColor(@PathVariable Integer id,
                            @ModelAttribute Color details,
                            RedirectAttributes redirectAttributes) {
        colorService.updateColor(id, details);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật màu thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/color/delete/{id}")
    public String deleteColor(@PathVariable Integer id,
                              RedirectAttributes redirectAttributes) {
        colorService.deleteColor(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa màu thành công!");
        return "redirect:/admin/categories";
    }
}