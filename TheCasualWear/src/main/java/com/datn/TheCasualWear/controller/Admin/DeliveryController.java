package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/delivery")
public class DeliveryController {

    private final OrderService orderService;

    public DeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping({"", "/"})
    public String deliveryPage(Model model) {
        model.addAttribute("shippingOrders", orderService.getShippingOrders());
        model.addAttribute("confirmedOrders",
                orderService.getOrdersByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("view", "delivery/orders");
        return "layouts/delivery-layout";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        model.addAttribute("view", "delivery/order-detail");
        return "layouts/delivery-layout";
    }

    @GetMapping("/orders/{id}/delivered")
    public String markDelivered(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {
        orderService.markDelivered(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đã giao!");
        return "redirect:/delivery";
    }
}