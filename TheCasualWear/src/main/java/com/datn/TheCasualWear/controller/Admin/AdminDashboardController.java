package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.OrderDetailRepository;
import com.datn.TheCasualWear.repository.ProductRepository;
import com.datn.TheCasualWear.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final AppUserService appUserService;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        List<AppOrder> allOrders = orderService.getAllOrders();

        // ==================== TỔNG QUAN ====================
        model.addAttribute("totalProducts",
                productService.getAdminProducts(null, 0).getTotalElements());
        model.addAttribute("totalUsers",
                appUserService.getAllUsers(null, 0).getTotalElements());
        model.addAttribute("totalOrders", allOrders.size());

        // Doanh thu (COMPLETED)
        double totalRevenue = allOrders.stream()
                .filter(o -> o != null && o.getStatus() == OrderStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice().doubleValue() : 0.0)
                .sum();
        model.addAttribute("totalRevenue", totalRevenue);

        // Tổng chi phí gốc (cost_price * quantity)
        double totalCost = orderDetailRepository.findAll().stream()
                .filter(od -> od != null
                        && od.getOrder() != null
                        && od.getOrder().getStatus() == OrderStatus.COMPLETED
                        && od.getProduct() != null
                        && od.getProduct().getCostPrice() != null)
                .mapToDouble(od -> od.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(
                                od.getQuantity() != null ? od.getQuantity() : 0))
                        .doubleValue())
                .sum();

        double totalProfit = totalRevenue - totalCost;
        model.addAttribute("totalProfit", totalProfit);

        // ==================== TUẦN NÀY ====================
        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();

        List<AppOrder> weekOrders = allOrders.stream()
                .filter(o -> o != null
                        && o.getStatus() == OrderStatus.COMPLETED
                        && o.getOrderDate() != null
                        && o.getOrderDate().isAfter(startOfWeek))
                .toList();

        double weekRevenue = weekOrders.stream()
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice().doubleValue() : 0.0)
                .sum();

        double weekCost = weekOrders.stream()
                .flatMap(o -> orderDetailRepository.findByOrderId(o.getId()).stream())
                .filter(od -> od != null
                        && od.getProduct() != null
                        && od.getProduct().getCostPrice() != null)
                .mapToDouble(od -> od.getProduct().getCostPrice()
                        .multiply(BigDecimal.valueOf(
                                od.getQuantity() != null ? od.getQuantity() : 0))
                        .doubleValue())
                .sum();

        model.addAttribute("weekRevenue", weekRevenue);
        model.addAttribute("weekProfit", weekRevenue - weekCost);
        model.addAttribute("weekOrders", weekOrders.size());

        // ==================== SẢN PHẨM BÁN CHẠY ====================
        // FIX: dùng lambda thay Integer::sum để tránh unboxing null (lỗi compiler warning dòng 90)
        Map<Product, Integer> soldMap = new LinkedHashMap<>();
        orderDetailRepository.findAll().stream()
                .filter(od -> od != null
                        && od.getOrder() != null
                        && od.getOrder().getStatus() == OrderStatus.COMPLETED
                        && od.getProduct() != null)
                .forEach(od -> soldMap.merge(
                        od.getProduct(),
                        od.getQuantity() != null ? od.getQuantity() : 0,
                        (existing, incoming) -> existing + incoming  // tránh NPE từ Integer::sum
                ));

        // Sort theo lượt bán giảm dần, lấy top 5
        List<Map.Entry<Product, Integer>> topSelling = soldMap.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(5)
                .toList();
        model.addAttribute("topSelling", topSelling);

        // ==================== SẮP HẾT HÀNG ====================
        List<Product> lowStock = productRepository
                .findByIsDeletedFalseAndStockLessThanAndStockGreaterThan(5, 0);
        List<Product> outOfStock = productRepository
                .findByIsDeletedFalseAndStock(0);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("outOfStock", outOfStock);

        // ==================== ĐƠN HÀNG MỚI NHẤT ====================
        model.addAttribute("recentOrders", allOrders.stream().limit(5).toList());
        model.addAttribute("pendingOrders",
                orderService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("shippingOrders",
                orderService.getOrdersByStatus(OrderStatus.SHIPPING).size());
        model.addAttribute("completedOrders",
                orderService.getOrdersByStatus(OrderStatus.COMPLETED).size());

        model.addAttribute("view", "admin/dashboard");
        return "layouts/admin-layout";
    }
}