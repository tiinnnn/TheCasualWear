package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.Product;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.OrderDetailRepository;
import com.datn.TheCasualWear.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService          orderService;
    private final ProductService        productService;
    private final ProductVariantService variantService;
    private final AppUserService        appUserService;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductSaleService       productSaleService;    // MỚI: hiệu quả sale
    private final StockMovementLogService  stockMovementLogService; // MỚI: nhập/xuất kho

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        List<AppOrder> allOrders = orderService.getAllOrders();

        //  TỔNG QUAN
        model.addAttribute("totalProducts",
                productService.getAdminProducts(null, 0).getTotalElements());
        model.addAttribute("totalUsers",
                appUserService.getAllUsers(null, null, 0).getTotalElements());
        model.addAttribute("totalOrders", allOrders.size());

        // Doanh thu (COMPLETED)
        double totalRevenue = allOrders.stream()
                .filter(o -> o != null && o.getStatus() == OrderStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalPrice() != null
                        ? o.getTotalPrice().doubleValue() : 0.0)
                .sum();
        model.addAttribute("totalRevenue", totalRevenue);

        // Chi phí gốc từ variant.costPrice
        double totalCost = orderDetailRepository.findAll().stream()
                .filter(od -> od != null
                        && od.getOrder() != null
                        && od.getOrder().getStatus() == OrderStatus.COMPLETED
                        && od.getVariant() != null
                        && od.getVariant().getCostPrice() != null)
                .mapToDouble(od -> od.getVariant().getCostPrice()
                        .multiply(BigDecimal.valueOf(
                                od.getQuantity() != null ? od.getQuantity() : 0))
                        .doubleValue())
                .sum();

        model.addAttribute("totalProfit", totalRevenue - totalCost);

        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();

        List<AppOrder> weekOrders = allOrders.stream()
                .filter(o -> o != null
                        && o.getStatus() == OrderStatus.COMPLETED
                        && o.getOrderDate() != null
                        && o.getOrderDate().isAfter(startOfWeek))
                .toList();

        double weekRevenue = weekOrders.stream()
                .mapToDouble(o -> o.getTotalPrice() != null
                        ? o.getTotalPrice().doubleValue() : 0.0)
                .sum();

        // Chi phí tuần từ variant.costPrice
        double weekCost = weekOrders.stream()
                .flatMap(o -> orderDetailRepository.findByOrderId(o.getId()).stream())
                .filter(od -> od != null
                        && od.getVariant() != null
                        && od.getVariant().getCostPrice() != null)
                .mapToDouble(od -> od.getVariant().getCostPrice()
                        .multiply(BigDecimal.valueOf(
                                od.getQuantity() != null ? od.getQuantity() : 0))
                        .doubleValue())
                .sum();

        model.addAttribute("weekRevenue", weekRevenue);
        model.addAttribute("weekProfit",  weekRevenue - weekCost);
        model.addAttribute("weekOrders",  weekOrders.size());

        //SẢN PHẨM BÁN CHẠY
        Map<Product, Integer> soldMap = new LinkedHashMap<>();
        orderDetailRepository.findAll().stream()
                .filter(od -> od != null
                        && od.getOrder() != null
                        && od.getOrder().getStatus() == OrderStatus.COMPLETED
                        && od.getVariant() != null
                        && od.getVariant().getProduct() != null)
                .forEach(od -> soldMap.merge(
                        od.getVariant().getProduct(),
                        od.getQuantity() != null ? od.getQuantity() : 0,
                        Integer::sum
                ));

        List<Map.Entry<Product, Integer>> topSelling = soldMap.entrySet().stream()
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(5)
                .toList();
        model.addAttribute("topSelling", topSelling);

        List<ProductVariant> lowStock   = variantService.getLowStockVariants();
        List<ProductVariant> outOfStock = variantService.getOutOfStockVariants();
        model.addAttribute("lowStock",   lowStock);
        model.addAttribute("outOfStock", outOfStock);

        model.addAttribute("recentOrders",   allOrders.stream().limit(5).toList());
        model.addAttribute("pendingOrders",
                orderService.getOrdersByStatus(OrderStatus.PENDING).size());
        model.addAttribute("shippingOrders",
                orderService.getOrdersByStatus(OrderStatus.SHIPPING).size());
        model.addAttribute("completedOrders",
                orderService.getOrdersByStatus(OrderStatus.COMPLETED).size());

        // ── DASHBOARD MỚI: doanh thu kênh / lý do hủy / hiệu quả sale / kho ──
        LocalDateTime monthStart = LocalDateTime.now()
                .withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 1) Doanh thu ONLINE vs COUNTER trong tháng
        model.addAttribute("revenueByChannel",
                orderService.getRevenueByChannel(monthStart, now));

        // 2) Thống kê lý do hủy/hoàn đơn + khách hủy/hoàn nhiều lần (>=3 lần)
        model.addAttribute("cancelReasonStats", orderService.getCancelReasonStats());
        model.addAttribute("frequentCancellers", orderService.getFrequentCancellers(3));

        // 3) Hiệu quả các đợt sale bắt đầu trong 30 ngày gần đây
        List<com.datn.TheCasualWear.entity.ProductSale> recentSales =
                productSaleService.getRecentSales(now.minusDays(30));
        model.addAttribute("saleEffectiveness",
                productSaleService.getSaleEffectiveness(recentSales));

        // 4) Nhập/xuất kho trong tháng, theo loại biến động
        model.addAttribute("stockMovementSummary",
                stockMovementLogService.getMovementSummary(monthStart, now));

        model.addAttribute("view", "admin/dashboard");
        return "layouts/admin-layout";
    }
}