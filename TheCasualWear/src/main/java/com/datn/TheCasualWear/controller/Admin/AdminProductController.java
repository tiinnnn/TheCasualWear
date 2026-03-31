package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SizeService sizeService;
    private final ColorService colorService;
    private final CloudinaryService cloudinaryService;

    public AdminProductController(ProductService productService,
                                  CategoryService categoryService,
                                  SizeService sizeService,
                                  ColorService colorService,
                                  CloudinaryService cloudinaryService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.sizeService = sizeService;
        this.colorService = colorService;
        this.cloudinaryService = cloudinaryService;
    }

    private void addFormData(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes", sizeService.getAllSizes());
        model.addAttribute("colors", colorService.getAllColors());
    }

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
                               Model model) {
        model.addAttribute("products", productService.getAdminProducts(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("view", "admin/product/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/deleted")
    public String deletedProducts(Model model) {
        model.addAttribute("deletedProducts", productService.getDeletedProducts());
        model.addAttribute("view", "admin/product/deleted");
        return "layouts/admin-layout";
    }

    @GetMapping("/add")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        addFormData(model);
        model.addAttribute("view", "admin/product/form");
        return "layouts/admin-layout";
    }

    @GetMapping("/edit/{id}")
    public String editProductPage(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        addFormData(model);
        model.addAttribute("view", "admin/product/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(value = "imageFiles", required = false)
                              List<MultipartFile> imageFiles,
                              RedirectAttributes redirectAttributes) throws Exception {
        if (product.getId() == null) {
            Product saved = productService.createProduct(product);
            if (imageFiles != null && !imageFiles.isEmpty()) {
                cloudinaryService.uploadProductImages(saved, imageFiles);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
        } else {
            productService.updateProduct(product.getId(), product);
            if (imageFiles != null && !imageFiles.isEmpty()) {
                cloudinaryService.uploadProductImages(
                        productService.getProductById(product.getId()), imageFiles);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm!");
        return "redirect:/admin/products";
    }

    @GetMapping("/restore/{id}")
    public String restoreProduct(@PathVariable Integer id,
                                 RedirectAttributes redirectAttributes) {
        productService.restoreProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm!");
        return "redirect:/admin/products/deleted";
    }

    @GetMapping("/image/delete/{imageId}")
    public String deleteImage(@PathVariable Integer imageId,
                              @RequestParam Integer productId,
                              RedirectAttributes redirectAttributes) throws Exception {
        cloudinaryService.deleteProductImage(imageId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh!");
        return "redirect:/admin/products/edit/" + productId;
    }
}