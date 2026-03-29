package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final AppUserService appUserService;
    private final AddressService addressService;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final VNPayService vnPayService;

    public OrderController(OrderService orderService,
                           AppUserService appUserService,
                           AddressService addressService,
                           CartService cartService,
                           VoucherService voucherService,
                           VNPayService vnPayService) {
        this.orderService = orderService;
        this.appUserService = appUserService;
        this.addressService = addressService;
        this.cartService = cartService;
        this.voucherService = voucherService;
        this.vnPayService = vnPayService;
    }

    private AppUser getCurrentUser(Authentication auth) {
        return appUserService.getUserByUsername(auth.getName());
    }

    // ==================== CHECKOUT ====================

    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);

        // Giỏ hàng trống thì về shop
        if (cartService.getCartItems(user).isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cartItems", cartService.getCartItems(user));
        model.addAttribute("totalPrice", cartService.getTotalPrice(user));
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        model.addAttribute("defaultAddress", addressService.getDefaultAddress(user));
        model.addAttribute("activeVouchers", voucherService.getActiveVouchers());
        model.addAttribute("view", "shop/checkout");
        return "layouts/shop-layout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam Integer shippingAddressId,
                             @RequestParam(required = false) Integer billingAddressId,
                             @RequestParam(required = false) String voucherCode,
                             @RequestParam String paymentMethod,
                             Authentication auth,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);

        Address shippingAddress = addressService.getAddressById(shippingAddressId, user);
        Address billingAddress = billingAddressId != null
                ? addressService.getAddressById(billingAddressId, user)
                : shippingAddress;

        AppOrder order = orderService.placeOrder(user, shippingAddress, billingAddress, voucherCode);

        // Thanh toán VNPay
        if ("VNPAY".equals(paymentMethod)) {
            request.getSession().setAttribute("pendingOrderId", order.getId());
            String paymentUrl = vnPayService.createPaymentUrl(
                    order.getTotalPrice().longValue(),
                    "Thanh toan don hang #" + order.getId(),
                    request
            );
            return "redirect:" + paymentUrl;
        }

        // COD
        redirectAttributes.addFlashAttribute("successMessage",
                "Đặt hàng thành công! Mã đơn hàng: #" + order.getId());
        return "redirect:/order/success/" + order.getId();
    }

    // ==================== VNPAY CALLBACK ====================

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (vnPayService.validateReturn(request)) {
            // Lấy order id từ session
            Integer orderId = (Integer) request.getSession().getAttribute("pendingOrderId");
            request.getSession().removeAttribute("pendingOrderId");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công! Mã đơn hàng: #" + orderId);
            return "redirect:/order/success/" + orderId;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thanh toán thất bại! Vui lòng thử lại.");
            return "redirect:/cart";
        }
    }

    // ==================== ĐẶT HÀNG THÀNH CÔNG ====================

    @GetMapping("/success/{id}")
    public String orderSuccess(@PathVariable Integer id,
                               Authentication auth,
                               Model model) {
        AppUser user = getCurrentUser(auth);
        AppOrder order = orderService.getOrderByIdAndUser(id, user);
        model.addAttribute("order", order);
        model.addAttribute("view", "shop/order-success");
        return "layouts/shop-layout";
    }

    // ==================== LỊCH SỬ ĐƠN HÀNG ====================

    @GetMapping("/history")
    public String orderHistory(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("orders", orderService.getOrdersByUser(user));
        model.addAttribute("view", "shop/order-history");
        return "layouts/shop-layout";
    }

    // ==================== CHI TIẾT ĐƠN HÀNG ====================

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Integer id,
                              Authentication auth,
                              Model model) {
        AppUser user = getCurrentUser(auth);
        AppOrder order = orderService.getOrderByIdAndUser(id, user);
        model.addAttribute("order", order);
        model.addAttribute("view", "shop/order-detail");
        return "layouts/shop-layout";
    }

    // ==================== XÁC NHẬN NHẬN HÀNG ====================

    @GetMapping("/confirm/{id}")
    public String confirmReceived(@PathVariable Integer id,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        orderService.confirmReceived(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận hàng thành công!");
        return "redirect:/order/detail/" + id;
    }

    // ==================== HỦY ĐƠN HÀNG ====================

    @GetMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Integer id,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        orderService.cancelOrder(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng thành công!");
        return "redirect:/order/history";
    }

    @GetMapping("/apply-voucher")
    @ResponseBody
    public Map<String, Object> applyVoucher(@RequestParam String code,
                                            @RequestParam Long total,
                                            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            AppUser user = getCurrentUser(auth);
            BigDecimal totalPrice = BigDecimal.valueOf(total);
            Voucher voucher = voucherService.applyVoucher(code, totalPrice, user);
            BigDecimal finalPrice = voucherService.calcDiscountedPrice(totalPrice, voucher);

            result.put("success", true);
            result.put("discountPercent", voucher.getDiscountPercent());
            result.put("finalPrice", String.format("%,.0f", finalPrice));
            result.put("finalPriceRaw", finalPrice.doubleValue());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}