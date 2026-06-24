package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.dto.OrderListDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService      orderService;
    private final AppUserRepository appUserRepository;

    public AdminOrderController(OrderService orderService,
                                AppUserRepository appUserRepository) {
        this.orderService      = orderService;
        this.appUserRepository = appUserRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // DANH SÁCH ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String fromDate,
                             @RequestParam(required = false) String toDate,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Page<OrderListDTO> orderPage =
                orderService.getOrderDTOs(keyword, status, fromDate, toDate, page);
        model.addAttribute("orders",         orderPage.getContent());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     orderPage.getTotalPages());
        model.addAttribute("totalItems",     orderPage.getTotalElements());
        model.addAttribute("keyword",        keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate",       fromDate);
        model.addAttribute("toDate",         toDate);
        model.addAttribute("statuses",       OrderStatus.values());
        model.addAttribute("view", "admin/order/list");
        return "layouts/admin-layout";
    }

    // ─────────────────────────────────────────────────────────────
    // CHI TIẾT ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        AppOrder order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("view", "admin/order/detail");
        return "layouts/admin-layout";
    }

    // ─────────────────────────────────────────────────────────────
    // XÁC NHẬN ĐƠN HÀNG  (PENDING → CONFIRMED)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Integer id, RedirectAttributes ra) {
        orderService.confirmOrder(id);
        ra.addFlashAttribute("successMessage", "Đã xác nhận đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // GỬI HÀNG CHO GHN  (CONFIRMED → SHIPPING)
    // Admin nhập mã vận đơn sau khi tạo đơn trên app/web GHN
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Integer id,
                            @RequestParam String trackingCode,
                            RedirectAttributes ra) {
        try {
            orderService.shipOrder(id, trackingCode);
            ra.addFlashAttribute("successMessage",
                    "Đã cập nhật mã vận đơn và chuyển trạng thái sang đang giao!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // XÁC NHẬN GIAO THÀNH CÔNG  (SHIPPING → DELIVERED)
    // Admin bấm khi GHN báo giao thành công hoặc khách phản hồi
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delivered")
    public String markDelivered(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            orderService.markDeliveredByAdmin(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận giao hàng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // HỦY ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id, RedirectAttributes ra) {
        orderService.cancelOrderByAdmin(id);
        ra.addFlashAttribute("successMessage", "Đã hủy đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // HOÀN HÀNG  (DELIVERED / COMPLETED → CANCELLED + restock)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/return")
    public String returnOrder(@PathVariable Integer id,
                              @RequestParam(defaultValue = "false") boolean restock,
                              RedirectAttributes ra) {
        try {
            orderService.returnOrder(id, restock);
            ra.addFlashAttribute("successMessage",
                    restock ? "Đã hoàn hàng và cộng lại stock!"
                            : "Đã hoàn hàng, không restock!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}