package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.dto.RevenueSummaryDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService          orderService;
    private final ProductService        productService;
    private final ProductVariantService variantService;
    private final AppUserService        appUserService;
    private final ProductSaleService       productSaleService;    // MỚI: hiệu quả sale
    private final StockMovementLogService  stockMovementLogService; // MỚI: nhập/xuất kho

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to) {
        List<AppOrder> allOrders = orderService.getAllOrders();

        //  TỔNG QUAN (số liệu toàn thời gian — không phụ thuộc bộ lọc)
        model.addAttribute("totalProducts",
                productService.getAdminProducts(null, 0).getTotalElements());
        model.addAttribute("totalUsers",
                appUserService.getAllUsers(null, null, 0).getTotalElements());

        // Doanh thu/lợi nhuận theo khoảng thời gian tùy chọn (2.4/2.5):
        // POS (COMPLETED) + Online VNPay đã thanh toán + Online COD đã hoàn tất.
        // Không điền from/to → mặc định toàn bộ lịch sử.
        LocalDateTime fromDateTime = (from == null || from.isBlank())
                ? null : LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDateTime = (to == null || to.isBlank())
                ? null : LocalDate.parse(to).atTime(23, 59, 59);

        RevenueSummaryDTO revenueSummary = orderService.getRevenueSummary(fromDateTime, toDateTime);
        model.addAttribute("revenueSummary", revenueSummary);
        model.addAttribute("totalRevenue", revenueSummary.totalRevenue());
        model.addAttribute("totalProfit",  revenueSummary.totalProfit());
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo",   to);

        // Đơn hàng theo đúng khoảng thời gian đang lọc (đồng bộ với revenueSummary ở trên).
        // Trước đây dùng allOrders.size() nên luôn ra tổng toàn bộ lịch sử, không đổi theo filter.
        long totalOrdersInRange = allOrders.stream()
                .filter(o -> (fromDateTime == null || !o.getOrderDate().isBefore(fromDateTime))
                        && (toDateTime   == null || !o.getOrderDate().isAfter(toDateTime)))
                .count();
        model.addAttribute("totalOrders", totalOrdersInRange);

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