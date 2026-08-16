package com.datn.TheCasualWear.controller.Admin;

import com.datn.TheCasualWear.dto.OrderListDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.service.OrderEmailService;
import com.datn.TheCasualWear.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService      orderService;
    private final AppUserRepository appUserRepository;
    private final OrderEmailService orderEmailService; // MỚI (4.3): gửi email xác nhận đơn

    public AdminOrderController(OrderService orderService,
                                AppUserRepository appUserRepository,
                                OrderEmailService orderEmailService) {
        this.orderService      = orderService;
        this.appUserRepository = appUserRepository;
        this.orderEmailService = orderEmailService;
    }

    // ─────────────────────────────────────────────────────────────
    // DANH SÁCH ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────
    // CHI TIẾT ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        AppOrder order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("cancelReasons", CancelReason.values());
        model.addAttribute("view", "admin/order/detail");
        return "layouts/admin-layout";
    }

    // ─────────────────────────────────────────────────────────────
    // XÁC NHẬN ĐƠN HÀNG  (PENDING → CONFIRMED)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Integer id, RedirectAttributes ra) {
        orderService.confirmOrder(id);

        // MỚI (4.3): gửi email xác nhận đơn — bất đồng bộ (mailTaskExecutor,
        // xem MailAsyncConfig), không chờ gửi xong mới trả response. Đơn đã
        // CONFIRMED thành công ở dòng trên rồi nên lỗi gửi mail (SMTP down...)
        // không ảnh hưởng tới việc xác nhận đơn — chỉ log lại bên trong
        // OrderEmailService.
        //
        // Dùng getOrderForEmail() (không phải getOrderById()) — method gửi
        // mail chạy trên thread khác (async), không còn Hibernate session của
        // request này nữa, nên mọi field LAZY (customer, shippingAddress,
        // orderDetails, variant...) phải được load sẵn trước khi băng qua
        // thread khác, nếu không sẽ lỗi LazyInitializationException lúc gửi.
        AppOrder order = orderService.getOrderForEmail(id);
        orderEmailService.sendOrderConfirmationAsync(order);

        ra.addFlashAttribute("successMessage", "Đã xác nhận đơn hàng!");
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // GỬI HÀNG CHO GHN  (CONFIRMED → SHIPPING)
    // Admin nhập mã vận đơn sau khi tạo đơn trên app/web GHN
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/ship")
    public String shipOrder(@PathVariable Integer id,
                            @RequestParam String trackingCode,
                            RedirectAttributes ra) {
        try {
            orderService.shipOrder(id, trackingCode);
            ra.addFlashAttribute("successMessage",
                    "Đã cập nhật mã vận đơn và chuyển trạng thái sang đang giao!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // ĐÁNH DẤU HOÀN THÀNH  (SHIPPING → COMPLETED)
    // Admin tự kiểm tra trạng thái trên GHN rồi đánh dấu — không còn
    // bước DELIVERED trung gian, không chờ khách xác nhận.
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/complete")
    public String completeOrder(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            orderService.completeOrderByAdmin(id);
            ra.addFlashAttribute("successMessage", "Đã đánh dấu đơn hàng hoàn thành!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // HỦY ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              @RequestParam CancelReason reason,
                              @RequestParam(required = false) String note,
                              RedirectAttributes ra) {
        try {
            orderService.cancelOrderByAdmin(id, reason, note);
            ra.addFlashAttribute("successMessage", "Đã hủy đơn hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ─────────────────────────────────────────────────────────────
    // HOÀN HÀNG  (COMPLETED → RETURNED + restock)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/return")
    public String returnOrder(@PathVariable Integer id,
                              @RequestParam(defaultValue = "false") boolean restock,
                              @RequestParam CancelReason reason,
                              @RequestParam(required = false) String note,
                              RedirectAttributes ra) {
        try {
            orderService.returnOrder(id, restock, reason, note);
            ra.addFlashAttribute("successMessage",
                    restock ? "Đã hoàn hàng và cộng lại stock!"
                            : "Đã hoàn hàng, không restock!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}