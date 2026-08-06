package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    private final VariantImageService   variantImageService;
    private final ProductSaleService    productSaleService; // MỚI: hiển thị giá sale trong list admin

    private void addFormData(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("sizes",      sizeService.getAllSizes());
        model.addAttribute("colors",     colorService.getAllColors());
    }

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

        // MỚI: sale đang chạy cho các sản phẩm trong trang này, để hiển thị
        // giá gạch ngang trực tiếp trong bảng danh sách
        List<Integer> productIds = productPage.getContent().stream()
                .map(Product::getId).toList();
        model.addAttribute("activeSales", productSaleService.getActiveSalesByProductIds(productIds));

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
                ra.addFlashAttribute("successMessage",
                        "Thêm sản phẩm thành công! Hãy thêm biến thể bên dưới.");
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

    // TRANG QUẢN LÝ VARIANT

    @GetMapping("/{id}/variants")
    public String variantPage(@PathVariable Integer id, Model model) {
        Product product = productService.getProductById(id);
        List<ProductVariant> variants = variantService.getVariantsByProduct(id);
        model.addAttribute("product",    product);
        model.addAttribute("variants",   variants);
        model.addAttribute("totalStock", variantService.getTotalStock(id));
        addFormData(model);
        model.addAttribute("view", "admin/product/variants");
        return "layouts/admin-layout";
    }

    @PostMapping("/{id}/variants/add-batch")
    public String addVariantBatch(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer colorId,
            @RequestParam(value = "sizeId", required = false) List<Integer> sizeIds,
            @RequestParam(required = false) String skuPrefix,
            RedirectAttributes ra) {

        if (colorId == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn màu sắc!");
            return "redirect:/admin/products/" + id + "/variants";
        }
        if (sizeIds == null || sizeIds.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất 1 size!");
            return "redirect:/admin/products/" + id + "/variants";
        }

        Product product = productService.getProductById(id);
        int added = 0, skipped = 0;
        StringBuilder errors = new StringBuilder();
        List<Integer> createdVariantIds = new java.util.ArrayList<>();

        for (Integer sizeId : sizeIds) {
            ProductVariant v = new ProductVariant();
            Color c = new Color(); c.setId(colorId); v.setColor(c);
            Size  s = new Size();  s.setId(sizeId);  v.setSize(s);
            // Tồn kho + giá vốn luôn khởi tạo = 0 ở createVariant() — bắt buộc
            // nhập kho qua module Quản lý kho (GoodsReceiptService) để đảm bảo
            // audit trail đầy đủ và giá vốn tính bình quân gia quyền đúng.

            if (skuPrefix != null && !skuPrefix.isBlank()) {
                String sizeName = sizeService.getSizeById(sizeId).getName();
                v.setSku(skuPrefix.toUpperCase().trim() + "-" + sizeName.toUpperCase());
            } else {
                // Không để sku = null: cột sku là UNIQUE, và SQL Server chỉ cho
                // phép 1 giá trị NULL trong toàn cột — tạo variant thứ 2 trở đi
                // với sku null sẽ vi phạm unique constraint. Tự sinh SKU dự
                // phòng dựa trên product/color/size để đảm bảo luôn duy nhất.
                v.setSku("SP" + id + "-C" + colorId + "-S" + sizeId);
            }

            try {
                ProductVariant saved = variantService.createVariant(product, v);
                createdVariantIds.add(saved.getId());
                added++;
            } catch (IllegalArgumentException e) {
                skipped++;
                errors.append(e.getMessage()).append("; ");
            }
        }

        if (added > 0) {
            ra.addFlashAttribute("successMessage",
                    "Đã thêm " + added + " biến thể (tồn kho = 0)!" +
                            (skipped > 0 ? " Bỏ qua " + skipped + " trùng." : "") +
                            " Hãy tạo phiếu nhập kho để thêm số lượng.");
            // Chuyển thẳng sang trang nhập kho, pre-select sẵn sản phẩm + các
            // biến thể vừa tạo để admin không phải chọn lại thủ công.
            StringBuilder url = new StringBuilder(
                    "redirect:/admin/warehouse/receipts/add?productId=" + id);
            for (Integer vid : createdVariantIds) {
                url.append("&variantId=").append(vid);
            }
            return url.toString();
        }
        if (skipped > 0) {
            ra.addFlashAttribute("errorMessage", errors.toString());
        }
        return "redirect:/admin/products/" + id + "/variants";
    }

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

    // Endpoint updateStock (POST .../variants/{variantId}/stock) đã bị xóa —
    // set thẳng tồn kho không qua audit trail. Dùng module Quản lý kho
    // (/admin/warehouse/receipts, /admin/warehouse/stock-log) thay thế.

    @PostMapping("/{productId}/variants/{variantId}/images/upload")
    public String uploadVariantImages(
            @PathVariable Integer productId,
            @PathVariable Integer variantId,
            @RequestParam("imageFiles") List<MultipartFile> imageFiles,
            RedirectAttributes ra) {
        try {
            ProductVariant variant = variantService.getVariantById(variantId);
            variantImageService.uploadImages(variant, imageFiles);
            ra.addFlashAttribute("successMessage", "Đã upload ảnh cho biến thể!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi upload ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/variants";
    }

    @GetMapping("/{productId}/variants/{variantId}/images/delete/{imageId}")
    public String deleteVariantImage(
            @PathVariable Integer productId,
            @PathVariable Integer variantId,
            @PathVariable Integer imageId,
            RedirectAttributes ra) {
        try {
            variantImageService.deleteImage(imageId);
            ra.addFlashAttribute("successMessage", "Đã xóa ảnh biến thể!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi xóa ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/variants";
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