package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.dto.OrderListDTO;
import com.datn.TheCasualWear.dto.DeliveryStaffDTO;
import com.datn.TheCasualWear.entity.DeliveryProfile;
import com.datn.TheCasualWear.repository.DeliveryProfileRepository;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.OrderAssignment;
import com.datn.TheCasualWear.enums.AssignmentStatus;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService              orderService;
    private final AppUserRepository         appUserRepository;
    private final DeliveryProfileRepository deliveryProfileRepository;

    public AdminOrderController(OrderService orderService,
                                AppUserRepository appUserRepository,
                                DeliveryProfileRepository deliveryProfileRepository) {
        this.orderService              = orderService;
        this.appUserRepository         = appUserRepository;
        this.deliveryProfileRepository = deliveryProfileRepository;
    }

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    // ── Danh sach ─────────────────────────────────────────

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

    // ── Chi tiet ──────────────────────────────────────────

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        AppOrder order = orderService.getOrderById(id);
        model.addAttribute("order", order);

        // Lich su tat ca assignment
        List<OrderAssignment> history = orderService.getAssignmentHistory(id);
        model.addAttribute("assignmentHistory", history);

        // So lan that bai
        long failCount = history.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.FAILED)
                .count();
        model.addAttribute("failCount", failCount);

        // Assignment dang active
        history.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
                .findFirst()
                .ifPresent(a -> model.addAttribute("currentAssignment", a));

        // Load delivery staffs kem profile (area, isAvailable)
        boolean canAssign = (order.getStatus() == OrderStatus.CONFIRMED && failCount < 4)
                || order.getStatus() == OrderStatus.SHIPPING; // reassign
        if (canAssign) {
            List<DeliveryStaffDTO> deliveryStaffs = appUserRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream()
                            .anyMatch(r -> r.getName().equals("ROLE_DELIVERY")))
                    .map(u -> new DeliveryStaffDTO(u,
                            deliveryProfileRepository.findByUserId(u.getId()).orElse(null)))
                    // Chi hien delivery dang available
                    .filter(DeliveryStaffDTO::isAvailable)
                    .toList();
            model.addAttribute("deliveryStaffs", deliveryStaffs);
        }

        model.addAttribute("view", "admin/order/detail");
        return "layouts/admin-layout";
    }

    // ── Trang thai ────────────────────────────────────────

    @GetMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Integer id,
                               RedirectAttributes ra) {
        orderService.confirmOrder(id);
        ra.addFlashAttribute("successMessage", "Đã xác nhận đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              RedirectAttributes ra) {
        orderService.cancelOrderByAdmin(id);
        ra.addFlashAttribute("successMessage", "Đã hủy đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    // ── Hoan hang (DELIVERED/COMPLETED trong 15 ngay) ────

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

    // ── Phan cong delivery ────────────────────────────────

    @PostMapping("/{id}/assign")
    public String assignOrder(@PathVariable Integer id,
                              @RequestParam Integer deliveryId,
                              RedirectAttributes ra) {
        try {
            orderService.assignOrder(id, deliveryId, getCurrentUser());
            ra.addFlashAttribute("successMessage",
                    "Đã giao đơn hàng cho nhân viên thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // Doi nguoi giao - khong tinh lan that bai
    @PostMapping("/{id}/reassign")
    public String reassignOrder(@PathVariable Integer id,
                                @RequestParam Integer deliveryId,
                                RedirectAttributes ra) {
        try {
            orderService.reassignOrder(id, deliveryId, getCurrentUser());
            ra.addFlashAttribute("successMessage",
                    "Đã đổi người giao hàng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}