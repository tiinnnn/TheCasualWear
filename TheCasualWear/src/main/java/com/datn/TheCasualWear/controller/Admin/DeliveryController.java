package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.service.OrderService;
import lombok.RequiredArgsConstructor;
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

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã xác nhận thu tiền COD!");
        return "redirect:/delivery/orders/" + id;
    }
}