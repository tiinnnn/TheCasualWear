package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Quản lý đơn hàng phía Admin.
 *
 * Template order/detail hiển thị order.orderDetails — mỗi OrderDetail giờ có
 * trường variant (ProductVariant) chứa đủ size + color + costPrice.
 * Không cần thay đổi logic controller; chỉ cần template đọc thêm:
 *   [[${item.variant.size.name}]] / [[${item.variant.color.name}]]
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final AppUserRepository appUserRepository;

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    //Danh sách
    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Page<AppOrder> orderPage = orderService.getAllOrders(keyword, status, page);
        model.addAttribute("orders",         orderPage.getContent());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     orderPage.getTotalPages());
        model.addAttribute("totalItems",     orderPage.getTotalElements());
        model.addAttribute("keyword",        keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses",       OrderStatus.values());
        model.addAttribute("view", "admin/order/list");
        return "layouts/admin-layout";
    }

    //Chi tiết
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        AppOrder order = orderService.getOrderById(id);
        model.addAttribute("order", order);

        if (order.getStatus() == OrderStatus.SHIPPING) {
            List<AppUser> deliveryStaffs = appUserRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream()
                            .anyMatch(r -> r.getName().equals("ROLE_DELIVERY")))
                    .toList();
            model.addAttribute("deliveryStaffs", deliveryStaffs);

            orderService.getAssignmentByOrderId(id)
                    .ifPresent(a -> model.addAttribute("currentAssignment", a));
        }

        model.addAttribute("view", "admin/order/detail");
        return "layouts/admin-layout";
    }

    //Thay đổi trạng thái

    @GetMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Integer id,
                               RedirectAttributes redirectAttributes) {
        orderService.confirmOrder(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/ship")
    public String shipOrder(@PathVariable Integer id,
                            RedirectAttributes redirectAttributes) {
        orderService.shipOrder(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã chuyển sang trạng thái đang giao!");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              RedirectAttributes redirectAttributes) {
        orderService.cancelOrderByAdmin(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignOrder(@PathVariable Integer id,
                              @RequestParam Integer deliveryId,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.assignOrder(id, deliveryId, getCurrentUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã giao đơn hàng cho nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
