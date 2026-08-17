package com.datn.TheCasualWear.controller.admin;

import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.SaleBatch;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.service.SaleBatchService;
import com.datn.TheCasualWear.service.SaleBatchService.BulkSaleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin: Sale theo nhóm sản phẩm (4.4) — bản rút gọn "sale theo đợt".
 * Không phải category sale — admin chọn tay nhiều sản phẩm cùng lúc,
 * set chung 1 mức % và 1 khoảng thời gian, hệ thống tạo N dòng
 * product_sale (mỗi sản phẩm 1 dòng), nhóm lại qua sale_batch_id.
 *
 * ProductRepository dùng đúng method có sẵn: findByIsDeletedFalse()
 * (đã xác nhận trong file thật) — không cần thêm method mới, sort theo
 * tên thực hiện ở tầng controller bằng Comparator.
 */
@Controller
@RequestMapping("/admin/sale-batches")
@RequiredArgsConstructor
public class AdminSaleBatchController {

    private final SaleBatchService saleBatchService;
    private final ProductRepository productRepository;

    @GetMapping
    public String list(Model model) {
        List<SaleBatch> batches = saleBatchService.getAllBatches();
        Map<Integer, Long> productCounts = batches.stream()
                .collect(Collectors.toMap(SaleBatch::getId,
                        b -> saleBatchService.countProductsInBatch(b.getId())));

        // MỚI: sale_batch không có cột trạng thái riêng — trạng thái thật
        // nằm ở is_active của các dòng product_sale con. Không có map này,
        // trang list hiện "Đang chạy" mãi kể cả sau khi bấm "Huỷ sớm", vì
        // badge cũ chỉ tính từ startDate/endDate của chính sale_batch.
        Map<Integer, Boolean> activeStatus = batches.stream()
                .collect(Collectors.toMap(SaleBatch::getId,
                        b -> saleBatchService.isBatchStillActive(b.getId())));

        model.addAttribute("batches", batches);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("activeStatus", activeStatus);
        model.addAttribute("now", LocalDateTime.now());
        model.addAttribute("view", "admin/sale-batch/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        List<Product> products = productRepository.findByIsDeletedFalse().stream()
                .sorted(java.util.Comparator.comparing(Product::getName))
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("view", "admin/sale-batch/form");
        return "layouts/admin-layout";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) List<Integer> productIds,
                         @RequestParam BigDecimal discountPercent,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                         RedirectAttributes redirectAttributes) {

        try {
            List<Product> products = (productIds == null || productIds.isEmpty())
                    ? List.of()
                    : productRepository.findAllById(productIds);

            BulkSaleResult result = saleBatchService.createSaleBatch(
                    name, products, discountPercent, startDate, endDate);

            redirectAttributes.addFlashAttribute("success",
                    "Đã tạo đợt sale \"" + name + "\" cho " + result.created().size() + " sản phẩm.");
            if (!result.skipped().isEmpty()) {
                redirectAttributes.addFlashAttribute("skipped", result.skipped());
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/sale-batches/new";
        }
        return "redirect:/admin/sale-batches";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        saleBatchService.deactivateBatch(id);
        redirectAttributes.addFlashAttribute("success", "Đã huỷ đợt sale.");
        return "redirect:/admin/sale-batches";
    }
}