package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService        productService;
    private final ProductVariantService variantService;
    private final CategoryService       categoryService;
    private final SizeService           sizeService;
    private final ColorService          colorService;
    private final CloudinaryService     cloudinaryService;

    private void addFormData(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes",      sizeService.getAllSizes());
        model.addAttribute("colors",     colorService.getAllColors());
    }

    // =========================================================
    // DANH SÁCH
    // =========================================================

    @GetMapping
    public String listProducts(@RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        Page<Product> productPage = productService.getAdminProducts(keyword, page);
        model.addAttribute("products",    productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  productPage.getTotalPages());
        model.addAttribute("totalItems",  productPage.getTotalElements());
        model.addAttribute("keyword",     keyword);
        model.addAttribute("view", "admin/product/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/deleted")
    public String deletedProducts(Model model) {
        model.addAttribute("deletedProducts", productService.getDeletedProducts());
        model.addAttribute("view", "admin/product/deleted");
        return "layouts/admin-layout";
    }

    // =========================================================
    // FORM TẠO / SỬA PRODUCT
    // =========================================================

    @GetMapping("/add")
    public String addProductPage(Model model) {
        model.addAttribute("product", new Product());
        addFormData(model);
        model.addAttribute("view", "admin/product/form");
        return "layouts/admin-layout";
    }

    @GetMapping("/edit/{id}")
    public String editProductPage(@PathVariable Integer id, Model model) {
        model.addAttribute("product",  productService.getProductById(id));
        model.addAttribute("variants", variantService.getVariantsByProduct(id));
        addFormData(model);
        model.addAttribute("view", "admin/product/form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String saveProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "imageFiles",      required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "copiedImageUrls", required = false) List<String> copiedImageUrls,
            RedirectAttributes ra) {
        try {
            if (product.getId() == null) {
                Product saved = productService.createProduct(product);
                if (copiedImageUrls != null && !copiedImageUrls.isEmpty())
                    cloudinaryService.copyImagesFromUrls(saved, copiedImageUrls);
                if (imageFiles != null && !imageFiles.isEmpty() && !imageFiles.get(0).isEmpty())
                    cloudinaryService.uploadProductImages(saved, imageFiles);
                ra.addFlashAttribute("successMessage", "Thêm sản phẩm thành công! Hãy thêm biến thể bên dưới.");
                // Redirect thẳng vào trang variants sau khi tạo mới
                return "redirect:/admin/products/" + saved.getId() + "/variants";
            } else {
                productService.updateProduct(product.getId(), product);
                if (imageFiles != null && !imageFiles.isEmpty() && !imageFiles.get(0).isEmpty())
                    cloudinaryService.uploadProductImages(
                            productService.getProductById(product.getId()), imageFiles);
                ra.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
                return "redirect:/admin/products";
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return product.getId() == null
                    ? "redirect:/admin/products/add"
                    : "redirect:/admin/products/edit/" + product.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // =========================================================
    // TRANG QUẢN LÝ VARIANT
    // =========================================================

    /**
     * Trang chính: hiển thị danh sách variant theo nhóm màu,
     * form thêm variant mới (chọn màu → tick nhiều size → nhập stock từng size).
     */
    @GetMapping("/{id}/variants")
    public String variantPage(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        List<ProductVariant> variants = variantService.getVariantsByProduct(id);
        model.addAttribute("product",  product);
        model.addAttribute("variants", variants);
        model.addAttribute("totalStock", variantService.getTotalStock(id));
        addFormData(model);
        model.addAttribute("view", "admin/product/variants");
        return "layouts/admin-layout";
    }

    /**
     * Thêm nhiều variant cùng lúc: 1 màu + nhiều size, mỗi size có stock riêng.
     * Form gửi:
     *   colorId        — 1 giá trị
     *   sizeId[]       — mảng size được tick
     *   stock[{sizeId}]— stock cho từng sizeId
     *   costPrice      — giá vốn chung (áp dụng cho tất cả size trong batch)
     *   priceAdjustment— chênh lệch giá chung
     *   skuPrefix      — prefix SKU, hệ thống thêm -SIZE vào sau
     */
    @PostMapping("/{id}/variants/add-batch")
    public String addVariantBatch(
            @PathVariable Integer id,
            @RequestParam Integer colorId,
            @RequestParam(value = "sizeId", required = false) List<Integer> sizeIds,
            @RequestParam(value = "stockMap", required = false) List<Integer> stockValues,
            @RequestParam(required = false) BigDecimal costPrice,
            @RequestParam(required = false) BigDecimal priceAdjustment,
            @RequestParam(required = false) String skuPrefix,
            RedirectAttributes ra) {

        if (sizeIds == null || sizeIds.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất 1 size!");
            return "redirect:/admin/products/" + id + "/variants";
        }

        Product product = productService.getProductById(id);
        int added = 0, skipped = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < sizeIds.size(); i++) {
            Integer sizeId = sizeIds.get(i);
            Integer stock  = (stockValues != null && i < stockValues.size())
                    ? stockValues.get(i) : 0;

            ProductVariant v = new ProductVariant();

            Color c = new Color(); c.setId(colorId); v.setColor(c);
            Size  s = new Size();  s.setId(sizeId);  v.setSize(s);

            v.setStock(stock != null && stock >= 0 ? stock : 0);
            v.setCostPrice(costPrice);
            v.setPriceAdjustment(priceAdjustment);

            // SKU = prefix-SIZENAME hoặc tự sinh
            if (skuPrefix != null && !skuPrefix.isBlank()) {
                String sizeName = sizeService.getSizeById(sizeId).getName();
                v.setSku(skuPrefix.toUpperCase().trim() + "-" + sizeName.toUpperCase());
            }

            try {
                variantService.createVariant(product, v);
                added++;
            } catch (IllegalArgumentException e) {
                skipped++;
                errors.append(e.getMessage()).append("; ");
            }
        }

        if (added > 0)
            ra.addFlashAttribute("successMessage",
                    "Đã thêm " + added + " biến thể!" +
                            (skipped > 0 ? " Bỏ qua " + skipped + " trùng." : ""));
        if (skipped > 0 && added == 0)
            ra.addFlashAttribute("errorMessage", errors.toString());

        return "redirect:/admin/products/" + id + "/variants";
    }

    /** Sửa stock / costPrice / priceAdjustment của 1 variant (inline form). */
    @PostMapping("/{productId}/variants/edit/{variantId}")
    public String editVariant(@PathVariable Integer productId,
                              @PathVariable Integer variantId,
                              @ModelAttribute ProductVariant details,
                              RedirectAttributes ra) {
        try {
            variantService.updateVariant(variantId, details);
            ra.addFlashAttribute("successMessage", "Cập nhật biến thể thành công!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/variants";
    }

    /** Xóa variant. */
    @GetMapping("/{productId}/variants/delete/{variantId}")
    public String deleteVariant(@PathVariable Integer productId,
                                @PathVariable Integer variantId,
                                RedirectAttributes ra) {
        try {
            variantService.deleteVariant(variantId);
            ra.addFlashAttribute("successMessage", "Đã xóa biến thể!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/variants";
    }

    /** Cập nhật stock nhanh — AJAX. */
    @PostMapping("/{productId}/variants/{variantId}/stock")
    @ResponseBody
    public ResponseEntity<String> updateStock(@PathVariable Integer productId,
                                              @PathVariable Integer variantId,
                                              @RequestParam Integer stock) {
        try {
            variantService.updateStock(variantId, stock);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        productService.deleteProduct(id);
        ra.addFlashAttribute("successMessage", "Đã ẩn sản phẩm!");
        return "redirect:/admin/products";
    }

    @GetMapping("/restore/{id}")
    public String restoreProduct(@PathVariable Integer id, RedirectAttributes ra) {
        productService.restoreProduct(id);
        ra.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm!");
        return "redirect:/admin/products/deleted";
    }

    @GetMapping("/hard-delete/{id}")
    public String hardDeleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            productService.hardDeleteProduct(id);
            ra.addFlashAttribute("successMessage", "Đã xóa hoàn toàn sản phẩm!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/products/deleted";
    }

    @GetMapping("/copy/{id}")
    public String copyProduct(@PathVariable Integer id, Model model) {
        Product source = productService.getProductById(id);
        Product copy   = new Product();
        copy.setName(source.getName() + " (copy)");
        copy.setDescription(source.getDescription());
        copy.setPrice(source.getPrice());
        copy.setCategory(source.getCategory());
        copy.setImages(source.getImages());
        model.addAttribute("product",      copy);
        model.addAttribute("copiedImages", source.getImages());
        addFormData(model);
        model.addAttribute("view", "admin/product/form");
        return "layouts/admin-layout";
    }

    @GetMapping("/image/delete/{imageId}")
    public String deleteImage(@PathVariable Integer imageId,
                              @RequestParam Integer productId,
                              RedirectAttributes ra) throws Exception {
        cloudinaryService.deleteProductImage(imageId);
        ra.addFlashAttribute("successMessage", "Đã xóa ảnh!");
        return "redirect:/admin/products/edit/" + productId;
    }
}