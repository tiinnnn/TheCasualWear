package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final AppUserService appUserService;

    public AdminDashboardController(OrderService orderService,
                                    ProductService productService,
                                    AppUserService appUserService) {
        this.orderService = orderService;
        this.productService = productService;
        this.appUserService = appUserService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // Thống kê tổng quan
        model.addAttribute("totalProducts", productService.getAdminProducts().size());
        model.addAttribute("totalUsers", appUserService.getAllUsers().size());
        model.addAttribute("totalOrders", orderService.getAllOrders().size());
        model.addAttribute("pendingOrders", orderService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("shippingOrders", orderService.getOrdersByStatus(OrderStatus.SHIPPING).size());
        model.addAttribute("completedOrders", orderService.getOrdersByStatus(OrderStatus.COMPLETED).size());

        // Doanh thu tổng
        double totalRevenue = orderService.getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalPrice().doubleValue())
                .sum();
        model.addAttribute("totalRevenue", totalRevenue);

        // 5 đơn hàng mới nhất
        model.addAttribute("recentOrders", orderService.getAllOrders().stream()
                .limit(5).toList());

        model.addAttribute("view", "admin/dashboard");
        return "layouts/admin-layout";
    }
}