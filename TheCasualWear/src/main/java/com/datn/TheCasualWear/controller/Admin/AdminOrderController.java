package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Page<AppOrder> orderPage = orderService.getAllOrders(keyword, status, page);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalItems", orderPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("view", "admin/order/list");
        return "layouts/admin-layout";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        model.addAttribute("view", "admin/order/detail");
        return "layouts/admin-layout";
    }

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
        redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển sang trạng thái đang giao!");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              RedirectAttributes redirectAttributes) {
        orderService.cancelOrderByAdmin(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }
}