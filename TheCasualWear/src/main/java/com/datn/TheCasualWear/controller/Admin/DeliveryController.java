package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final OrderService orderService;
    private final AppOrderRepository orderRepository;
    private final AppUserRepository appUserRepository;

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    @GetMapping({"", "/"})
    public String deliveryPage(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String fromDate,
                               @RequestParam(required = false) String toDate,
                               Model model) {
        AppUser currentUser = getCurrentUser();
        String activeStatus = (status == null) ? "ASSIGNED" : status;

        model.addAttribute("myAssignments",
                orderService.getMyAssignments(currentUser,
                        "ALL".equals(activeStatus) ? null : activeStatus,
                        fromDate, toDate));
        model.addAttribute("selectedStatus", activeStatus);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("view", "delivery/orders");
        return "layouts/delivery-layout";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        AppUser currentUser = getCurrentUser();
        model.addAttribute("order", orderService.getOrderById(id));

        orderService.getAssignmentByOrderId(id)
                .filter(a -> a.getDeliveryStaff().getId().equals(currentUser.getId()))
                .ifPresent(a -> model.addAttribute("assignment", a));

        model.addAttribute("view", "delivery/order-detail");
        return "layouts/delivery-layout";
    }

    @GetMapping("/orders/{id}/delivered")
    public String markDelivered(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        AppUser currentUser = getCurrentUser();
        try {
            orderService.markDelivered(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật trạng thái đã giao!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/delivery";
    }

    @GetMapping("/orders/{id}/collected")
    public String markCollected(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        AppOrder order = orderService.getOrderById(id);
        if (!"COD".equals(order.getPaymentMethod())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đơn hàng này đã thanh toán qua VNPay!");
            return "redirect:/delivery/orders/" + id;
        }
        order.setIsPaid(true);
        orderRepository.save(order);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận thu tiền COD!");
        return "redirect:/delivery/orders/" + id;
    }

    @PostMapping("/orders/{id}/failed")
    public String markFailed(@PathVariable Integer id,
                             @RequestParam(required = false) String failReason,
                             RedirectAttributes redirectAttributes) {
        AppUser currentUser = getCurrentUser();
        try {
            String reason = (failReason == null || failReason.isBlank())
                    ? "Không có lý do" : failReason;
            orderService.markFailed(id, currentUser, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật giao hàng thất bại!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/delivery";
    }
}