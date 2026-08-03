package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.GoodsReceiptFormRequest;
import com.datn.TheCasualWear.dto.GoodsReceiptItemDTO;
import com.datn.TheCasualWear.dto.VariantOptionDTO;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.GoodsReceipt;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.GoodsReceiptService;
import com.datn.TheCasualWear.service.ProductService;
import com.datn.TheCasualWear.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/warehouse/receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService   goodsReceiptService;
    private final ProductService        productService;
    private final ProductVariantService variantService;
    private final AppUserRepository     appUserRepository;

    // Cùng pattern lấy user hiện tại như trong CashierService/OrderService
    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập");
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("receipts", goodsReceiptService.getAllReceipts());
        model.addAttribute("view", "admin/warehouse/receipt-list");
        return "layouts/admin-layout";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("form", new GoodsReceiptFormRequest());
        model.addAttribute("products", productService.getAdminProductsList());
        model.addAttribute("view", "admin/warehouse/receipt-form");
        return "layouts/admin-layout";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("form") GoodsReceiptFormRequest form,
                       RedirectAttributes ra) {
        try {
            List<GoodsReceiptItemDTO> items = form.getItems().stream()
                    .map(i -> new GoodsReceiptItemDTO(
                            i.getVariantId(), i.getQuantity(), i.getUnitCostPrice()))
                    .collect(Collectors.toList());

            GoodsReceipt receipt = goodsReceiptService.createReceipt(
                    form.getSupplierName(), form.getNote(), items, getCurrentUser());

            ra.addFlashAttribute("successMessage",
                    "Tạo phiếu nhập kho " + receipt.getCode() + " thành công!");
            return "redirect:/admin/warehouse/receipts/" + receipt.getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/warehouse/receipts/add";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("receipt", goodsReceiptService.getReceiptById(id));
        model.addAttribute("view", "admin/warehouse/receipt-detail");
        return "layouts/admin-layout";
    }

    // AJAX: JS gọi khi admin chọn 1 Product trong form nhập kho,
    // để load danh sách variant (size/màu/tồn hiện tại) tương ứng.
    @GetMapping("/variant-options")
    @ResponseBody
    public List<VariantOptionDTO> variantOptions(@RequestParam Integer productId) {
        List<ProductVariant> variants = variantService.getVariantsByProduct(productId);
        return variants.stream()
                .map(v -> new VariantOptionDTO(
                        v.getId(),
                        v.getSku(),
                        v.getSize()  != null ? v.getSize().getName()  : null,
                        v.getColor() != null ? v.getColor().getName() : null,
                        v.getStock()
                ))
                .collect(Collectors.toList());
    }
}
