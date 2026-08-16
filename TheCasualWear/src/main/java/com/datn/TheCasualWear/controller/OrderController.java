package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.config.VNPayConfig;
import com.datn.TheCasualWear.dto.GuestCartItem;
import com.datn.TheCasualWear.dto.GuestCheckoutFormDTO;
import com.datn.TheCasualWear.dto.GuestOrderLookupDTO;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final AppUserService appUserService;
    private final AddressService addressService;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final VNPayService vnPayService;
    private final ProductSaleService productSaleService; // badge/giá sale cho trang checkout
    private final GuestCartService guestCartService; // MỚI: giỏ hàng khách vãng lai (4.1)
    private final OrderLookupRateLimiter orderLookupRateLimiter; // MỚI: chống brute-force tra cứu (4.2/6.6)

    public OrderController(OrderService orderService,
                           AppUserService appUserService,
                           AddressService addressService,
                           CartService cartService,
                           VoucherService voucherService,
                           VNPayService vnPayService,
                           ProductSaleService productSaleService,
                           GuestCartService guestCartService,
                           OrderLookupRateLimiter orderLookupRateLimiter) {
        this.orderService = orderService;
        this.appUserService = appUserService;
        this.addressService = addressService;
        this.cartService = cartService;
        this.voucherService = voucherService;
        this.vnPayService = vnPayService;
        this.productSaleService = productSaleService;
        this.guestCartService = guestCartService;
        this.orderLookupRateLimiter = orderLookupRateLimiter;
    }

    private AppUser getCurrentUser(Authentication auth) {
        return appUserService.getUserByUsername(auth.getName());
    }

    //CHECKOUT (user đã đăng nhập)
    @GetMapping("/checkout")
    public String checkoutPage(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);

        // Giỏ hàng trống thì về shop
        if (cartService.getCartItems(user).isEmpty()) {
            return "redirect:/cart";
        }

        List<CartItem> cartItems = cartService.getCartItems(user);

        // Giá đã áp sale cho từng dòng — khớp với totalPrice bên dưới, và
        // khớp với cách tính unitPrice lúc snapshot vào order_detail
        // (OrderService.placeOrder cũng gọi productSaleService.getEffectivePrice).
        Map<Integer, BigDecimal> itemPrices = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            itemPrices.put(item.getId(), cartService.getEffectiveUnitPrice(item));
        }
        List<Integer> productIds = cartItems.stream()
                .map(i -> i.getVariant().getProduct().getId())
                .distinct().toList();
        Map<Integer, ProductSale> activeSales = productSaleService.getActiveSalesByProductIds(productIds);

        long totalPrice = cartService.getTotalPrice(user);
        long grandTotal = totalPrice + OrderService.SHIPPING_FEE.longValue();

        // Chỉ hiển thị voucher đủ điều kiện áp dụng (order.total >= voucher.minOrderValue)
        BigDecimal totalPriceBD = BigDecimal.valueOf(totalPrice);
        List<Voucher> eligibleVouchers = voucherService.getActiveVouchers().stream()
                .filter(v -> v.getMinOrderValue() == null
                        || totalPriceBD.compareTo(v.getMinOrderValue()) >= 0)
                .toList();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("itemPrices", itemPrices);
        model.addAttribute("activeSales", activeSales);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", OrderService.SHIPPING_FEE);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        model.addAttribute("defaultAddress", addressService.getDefaultAddress(user));
        model.addAttribute("activeVouchers", eligibleVouchers);
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

        // Validate COD > 1 triệu (áp dụng trên tổng tiền hàng, chưa gồm phí ship)
        long totalPrice = cartService.getTotalPrice(user);
        long grandTotal = totalPrice + OrderService.SHIPPING_FEE.longValue();
        if (totalPrice > 1000000 && "COD".equals(paymentMethod)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đơn hàng trên 1.000.000 đ bắt buộc thanh toán qua ngân hàng!");
            return "redirect:/order/checkout";
        }

        if ("VNPAY".equals(paymentMethod)) {
            // Lưu vào session để dùng sau khi VNPay callback
            HttpSession session = request.getSession();
            session.setAttribute("pendingShippingAddressId", shippingAddressId);
            session.setAttribute("pendingBillingAddressId",
                    billingAddressId != null ? billingAddressId : shippingAddressId);
            session.setAttribute("pendingVoucherCode", voucherCode);

            // Số tiền thanh toán qua VNPay phải gồm cả phí ship
            String paymentUrl = vnPayService.createPaymentUrl(
                    grandTotal,
                    "Thanh toan don hang",
                    request
            );
            return "redirect:" + paymentUrl;
        }

        // COD — tạo order bình thường
        Address shippingAddress = addressService.getAddressById(shippingAddressId, user);
        Address billingAddress  = billingAddressId != null
                ? addressService.getAddressById(billingAddressId, user)
                : shippingAddress;

        AppOrder order = orderService.placeOrder(user, shippingAddress,
                billingAddress, voucherCode, "COD");
        redirectAttributes.addFlashAttribute("successMessage",
                "Đặt hàng thành công! Mã đơn hàng: #" + order.getOrderCode());
        return "redirect:/order/success/" + order.getId();
    }

    // ══════════════════════════════════════════════════════════════════
    // CHECKOUT KHÁCH VÃNG LAI (4.1) — song song, không đụng luồng user ở trên
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/checkout-guest")
    public String checkoutGuestPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        List<GuestCartItem> cartItems = guestCartService.getCart(session);

        if (cartItems.isEmpty()) {
            return "redirect:/cart-guest";
        }

        BigDecimal totalPrice = guestCartService.getTotalPrice(session);
        BigDecimal grandTotal = totalPrice.add(OrderService.SHIPPING_FEE);

        List<Voucher> eligibleVouchers = voucherService.getActiveVouchers().stream()
                .filter(v -> v.getMinOrderValue() == null
                        || totalPrice.compareTo(v.getMinOrderValue()) >= 0)
                .toList();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", OrderService.SHIPPING_FEE);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("activeVouchers", eligibleVouchers);
        model.addAttribute("guestCheckoutForm", new GuestCheckoutFormDTO());
        model.addAttribute("view", "shop/checkout-guest");
        return "layouts/shop-layout";
    }

    @PostMapping("/checkout-guest")
    public String placeOrderGuest(@Valid @ModelAttribute("guestCheckoutForm") GuestCheckoutFormDTO form,
                                  BindingResult bindingResult,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession();
        List<GuestCartItem> cartItems = guestCartService.getCart(session);

        if (cartItems.isEmpty()) {
            return "redirect:/cart-guest";
        }

        BigDecimal totalPrice = guestCartService.getTotalPrice(session);
        BigDecimal grandTotal = totalPrice.add(OrderService.SHIPPING_FEE);

        // Validate COD > 1 triệu, giống luồng user đã login
        if (totalPrice.compareTo(BigDecimal.valueOf(1_000_000)) > 0
                && "COD".equals(form.getPaymentMethod())) {
            bindingResult.reject("codLimitExceeded",
                    "Đơn hàng trên 1.000.000 đ bắt buộc thanh toán qua ngân hàng!");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("shippingFee", OrderService.SHIPPING_FEE);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("activeVouchers", voucherService.getActiveVouchers());
            model.addAttribute("view", "shop/checkout-guest");
            return "layouts/shop-layout";
        }

        if ("VNPAY".equals(form.getPaymentMethod())) {
            // Lưu form vào session để tạo order sau khi VNPay callback về
            // (giống pendingShippingAddressId... ở luồng user phía trên).
            // KHÔNG clear giỏ hàng ở đây — giỏ hàng (GuestCartService, cũng
            // lưu trong session) phải giữ nguyên tới khi thanh toán thành
            // công, order chỉ thực sự được tạo ở vnpayReturnGuest().
            session.setAttribute("pendingGuestCheckoutForm", form);

            // Return URL riêng cho guest — không dùng vnPayConfig.getReturnUrl()
            // mặc định (đang trỏ về /order/vnpay-return của luồng user).
            String guestReturnUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(request.getContextPath() + "/order/vnpay-return-guest")
                    .replaceQuery(null)
                    .build()
                    .toUriString();

            String paymentUrl = vnPayService.createPaymentUrl(
                    grandTotal.longValue(),
                    "Thanh toan don hang khach vang lai",
                    VNPayConfig.getRandomNumber(8),
                    guestReturnUrl,
                    request
            );
            return "redirect:" + paymentUrl;
        }

        // COD — tạo order bình thường
        AppOrder order = orderService.placeOrderGuest(form, cartItems);
        guestCartService.clearCart(session);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đặt hàng thành công! Mã đơn hàng: #" + order.getOrderCode());
        return "redirect:/order/success-guest/" + order.getOrderCode();
    }

    // Trang thành công riêng cho guest — tra theo orderCode (không phải id),
    // vì guest không có quyền truy vấn theo id nội bộ (xem 6.6). Dùng template
    // riêng order-success-guest.html (không phải order-success.html của user
    // đã login) vì trang đó có nút "Xem chi tiết đơn hàng" trỏ /order/detail/{id}
    // — route yêu cầu login và dùng id nội bộ, cả 2 điều guest đều không có.
    @GetMapping("/success-guest/{orderCode}")
    public String orderSuccessGuest(@PathVariable String orderCode, Model model) {
        AppOrder order = orderService.getOrderByCode(orderCode);
        model.addAttribute("order", order);
        model.addAttribute("maskedPhone", maskPhone(order.getShippingAddress().getPhone()));
        model.addAttribute("maskedEmail", maskEmail(order.getGuestEmail()));
        model.addAttribute("view", "shop/order-success-guest");
        return "layouts/shop-layout";
    }

    // MỚI: VNPAY CALLBACK CHO KHÁCH VÃNG LAI — song song với vnpayReturn()
    // (luồng user) bên dưới. Guest không có Authentication nên không thể tái
    // sử dụng chung 1 endpoint; route này bắt buộc phải permitAll trong
    // SecurityConfig (xem ghi chú ở đó), vì VNPay redirect thẳng về đây mà
    // không mang theo session đăng nhập nào cả.
    @GetMapping("/vnpay-return-guest")
    public String vnpayReturnGuest(HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession();

        if (vnPayService.validateReturn(request)) {
            // Thanh toán thành công → MỚI tạo order (giống luồng user: order
            // chỉ được tạo SAU khi VNPay xác nhận, không tạo trước rồi chờ)
            GuestCheckoutFormDTO form =
                    (GuestCheckoutFormDTO) session.getAttribute("pendingGuestCheckoutForm");
            session.removeAttribute("pendingGuestCheckoutForm");

            List<GuestCartItem> cartItems = guestCartService.getCart(session);

            if (form == null || cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Phiên đặt hàng đã hết hạn! Vui lòng thử lại.");
                return "redirect:/cart-guest";
            }

            AppOrder order = orderService.placeOrderGuest(form, cartItems);
            guestCartService.clearCart(session);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công! Mã đơn hàng: #" + order.getOrderCode());
            return "redirect:/order/success-guest/" + order.getOrderCode();

        } else {
            // Thanh toán thất bại hoặc huỷ → KHÔNG tạo order, giỏ hàng vẫn còn
            session.removeAttribute("pendingGuestCheckoutForm");
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thanh toán thất bại hoặc bị hủy! Đơn hàng chưa được tạo.");
            return "redirect:/cart-guest";
        }
    }

    //VNPAY CALLBACK (user đã đăng nhập)
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (vnPayService.validateReturn(request)) {
            // Thanh toán thành công → MỚI tạo order
            AppUser user = getCurrentUser(auth);
            HttpSession session = request.getSession();

            Integer shippingAddressId = (Integer) session.getAttribute("pendingShippingAddressId");
            Integer billingAddressId  = (Integer) session.getAttribute("pendingBillingAddressId");
            String voucherCode        = (String)  session.getAttribute("pendingVoucherCode");

            // Xóa session
            session.removeAttribute("pendingShippingAddressId");
            session.removeAttribute("pendingBillingAddressId");
            session.removeAttribute("pendingVoucherCode");

            if (shippingAddressId == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Phiên đặt hàng đã hết hạn! Vui lòng thử lại.");
                return "redirect:/cart";
            }

            Address shippingAddress = addressService.getAddressById(shippingAddressId, user);
            Address billingAddress  = addressService.getAddressById(billingAddressId, user);

            AppOrder order = orderService.placeOrder(user, shippingAddress,
                    billingAddress, voucherCode, "VNPAY");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công! Mã đơn hàng: #" + order.getOrderCode());
            return "redirect:/order/success/" + order.getId();

        } else {
            // Thanh toán thất bại hoặc huỷ → KHÔNG tạo order, cart vẫn còn
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thanh toán thất bại hoặc bị hủy! Đơn hàng chưa được tạo.");
            return "redirect:/cart";
        }
    }

    //ĐẶT HÀNG THÀNH CÔNG
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

    //LỊCH SỬ ĐƠN HÀNG useless but i'll keep it anyway
    @GetMapping("/history")
    public String orderHistory(Authentication auth, Model model) {
        AppUser user = getCurrentUser(auth);
        model.addAttribute("orders", orderService.getOrdersByUser(user));
        model.addAttribute("view", "shop/order-history");
        return "layouts/shop-layout";
    }

    //CHI TIẾT ĐƠN HÀNG
    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Integer id,
                              Authentication auth,
                              Model model) {
        AppUser user = getCurrentUser(auth);
        AppOrder order = orderService.getOrderByIdAndUser(id, user);
        model.addAttribute("order", order);
        model.addAttribute("cancelReasons", CancelReason.values());
        model.addAttribute("view", "shop/order-detail");
        return "layouts/shop-layout";
    }

    //HỦY ĐƠN HÀNG
    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Integer id,
                              @RequestParam CancelReason reason,
                              @RequestParam(required = false) String note,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        try {
            orderService.cancelOrder(id, user, reason, note);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng thành công!");
            return "redirect:/account/orders";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/order/detail/" + id;
        }
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
            BigDecimal discountAmount = totalPrice.subtract(finalPrice);

            result.put("success", true);
            result.put("discountPercent", voucher.getDiscountPercent());
            result.put("maxDiscount", voucher.getMaxDiscount() != null
                    ? String.format("%,.0f", voucher.getMaxDiscount()) : null);
            result.put("discountAmount", String.format("%,.0f", discountAmount));
            result.put("finalPrice", String.format("%,.0f", finalPrice));
            result.put("finalPriceRaw", finalPrice.doubleValue());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════
    // TRA CỨU ĐƠN HÀNG KHÁCH VÃNG LAI (4.2 + 6.6)
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/lookup-guest")
    public String lookupGuestPage(Model model) {
        model.addAttribute("lookupForm", new GuestOrderLookupDTO());
        model.addAttribute("view", "shop/order-lookup-guest");
        return "layouts/shop-layout";
    }

    @PostMapping("/lookup-guest")
    public String lookupGuestSubmit(@Valid @ModelAttribute("lookupForm") GuestOrderLookupDTO form,
                                    BindingResult bindingResult,
                                    HttpServletRequest request,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("view", "shop/order-lookup-guest");
            return "layouts/shop-layout";
        }

        String rateLimitKey = orderLookupRateLimiter.keyFor(request);
        if (orderLookupRateLimiter.isBlocked(rateLimitKey)) {
            model.addAttribute("errorMessage",
                    "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau vài phút.");
            model.addAttribute("view", "shop/order-lookup-guest");
            return "layouts/shop-layout";
        }

        Optional<AppOrder> orderOpt = orderService.lookupGuestOrder(form.getOrderCode(), form.getContact());

        if (orderOpt.isEmpty()) {
            orderLookupRateLimiter.recordFailure(rateLimitKey);
            // Cùng 1 thông báo lỗi chung chung cho MỌI trường hợp không khớp
            // (sai mã đơn, sai SĐT/email, hay cả hai) — xem ghi chú ở
            // OrderService.lookupGuestOrder(), tránh lộ qua response việc mã
            // đơn có tồn tại hay không.
            model.addAttribute("errorMessage", "Không tìm thấy đơn hàng khớp với thông tin đã nhập.");
            model.addAttribute("view", "shop/order-lookup-guest");
            return "layouts/shop-layout";
        }

        orderLookupRateLimiter.recordSuccess(rateLimitKey);
        AppOrder order = orderOpt.get();

        // MỚI (6.6): mask SĐT/email khi hiển thị lại — trang kết quả chỉ dùng
        // maskedPhone/maskedEmail, KHÔNG in trực tiếp order.shippingAddress.phone
        // hay order.guestEmail ra view.
        model.addAttribute("order", order);
        model.addAttribute("maskedPhone", maskPhone(order.getShippingAddress().getPhone()));
        model.addAttribute("maskedEmail", maskEmail(order.getGuestEmail()));
        model.addAttribute("view", "shop/order-lookup-result");
        return "layouts/shop-layout";
    }

    // "090***4567" — giữ 3 số đầu + 4 số cuối, che phần giữa
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }

    // "a***@example.com" — chỉ giữ ký tự đầu của phần local
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.isEmpty() ? "*" : local.charAt(0) + "***";
        return masked + "@" + parts[1];
    }


    // MỚI: apply-voucher cho guest — không có AppUser nên gọi voucherService
    // với user=null. ⚠️ CẦN XÁC NHẬN: voucherService.applyVoucher(code, total, user)
    // có chấp nhận user null không (VD nếu voucher có giới hạn "1 lần/khách"
    // dựa trên user, guest sẽ không check được điều đó bằng field này).
    @GetMapping("/apply-voucher-guest")
    @ResponseBody
    public Map<String, Object> applyVoucherGuest(@RequestParam String code,
                                                 @RequestParam Long total) {
        Map<String, Object> result = new HashMap<>();
        try {
            BigDecimal totalPrice = BigDecimal.valueOf(total);
            Voucher voucher = voucherService.applyVoucher(code, totalPrice, null);
            BigDecimal finalPrice = voucherService.calcDiscountedPrice(totalPrice, voucher);
            BigDecimal discountAmount = totalPrice.subtract(finalPrice);

            result.put("success", true);
            result.put("discountPercent", voucher.getDiscountPercent());
            result.put("maxDiscount", voucher.getMaxDiscount() != null
                    ? String.format("%,.0f", voucher.getMaxDiscount()) : null);
            result.put("discountAmount", String.format("%,.0f", discountAmount));
            result.put("finalPrice", String.format("%,.0f", finalPrice));
            result.put("finalPriceRaw", finalPrice.doubleValue());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}