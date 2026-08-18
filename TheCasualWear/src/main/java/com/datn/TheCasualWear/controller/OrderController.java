package com.datn.TheCasualWear.controller;

import com.datn.TheCasualWear.config.VNPayConfig;
import com.datn.TheCasualWear.dto.CustomerCheckoutFormDTO;
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
        BigDecimal totalPriceBD = BigDecimal.valueOf(totalPrice);

        // MỚI: form checkout nhận field địa chỉ trực tiếp (giống guest) thay
        // vì chỉ nhận shippingAddressId — prefill sẵn từ địa chỉ mặc định
        // (nếu có) để khách không phải gõ lại từ đầu, chỉ sửa khi cần.
        List<Address> savedAddresses = addressService.getAddressesByUser(user);
        Address defaultAddress = addressService.getDefaultAddress(user);

        // MỚI (4.5): phí ship ban đầu tính theo địa chỉ mặc định (nếu có) —
        // gọi GHN thật nếu địa chỉ đã có mã GHN, fallback region-based nếu
        // chưa/lỗi. Số này chỉ là ước tính lúc load trang; JS sẽ gọi lại
        // /order/shipping-fee-preview mỗi khi khách đổi tỉnh/quận-huyện/
        // phường-xã trong dropdown (xem checkout.html).
        BigDecimal shippingFee = (defaultAddress != null)
                ? orderService.calculateShippingFee(defaultAddress, totalPriceBD, orderService.getCartWeightGrams(user))
                : OrderService.DEFAULT_SHIPPING_FEE;
        long grandTotal = totalPrice + shippingFee.longValue();

        // Chỉ hiển thị voucher đủ điều kiện áp dụng (order.total >= voucher.minOrderValue)
        List<Voucher> eligibleVouchers = voucherService.getActiveVouchers().stream()
                .filter(v -> v.getMinOrderValue() == null
                        || totalPriceBD.compareTo(v.getMinOrderValue()) >= 0)
                .toList();

        CustomerCheckoutFormDTO checkoutForm = new CustomerCheckoutFormDTO();
        checkoutForm.setPaymentMethod("COD");
        // Chưa có địa chỉ nào thì tick sẵn "đặt làm mặc định" — xem
        // CustomerCheckoutFormDTO + AddressService.createAddressForOrder().
        checkoutForm.setSaveAsDefault(savedAddresses.isEmpty());
        if (defaultAddress != null) {
            checkoutForm.setUseExistingAddressId(defaultAddress.getId());
            checkoutForm.setFullName(defaultAddress.getFullName());
            checkoutForm.setPhone(defaultAddress.getPhone());
            checkoutForm.setStreet(defaultAddress.getStreet());
            checkoutForm.setCity(defaultAddress.getCity());
            checkoutForm.setDistrict(defaultAddress.getDistrict());
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("itemPrices", itemPrices);
        model.addAttribute("activeSales", activeSales);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("addresses", savedAddresses);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("checkoutForm", checkoutForm);
        model.addAttribute("activeVouchers", eligibleVouchers);
        model.addAttribute("view", "shop/checkout");
        return "layouts/shop-layout";
    }

    // MỚI: xác định Address dùng để giao hàng từ CustomerCheckoutFormDTO —
    // xem javadoc ở CustomerCheckoutFormDTO để biết quy tắc
    // useExistingAddressId vs nhập/sửa mới + saveAsDefault.
    private Address resolveShippingAddress(CustomerCheckoutFormDTO form, AppUser user) {
        if (form.getUseExistingAddressId() != null) {
            return addressService.getAddressById(form.getUseExistingAddressId(), user);
        }

        if (form.getFullName() == null || form.getFullName().isBlank()
                || form.getPhone() == null || form.getPhone().isBlank()
                || form.getStreet() == null || form.getStreet().isBlank()
                || form.getCity() == null || form.getCity().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin địa chỉ giao hàng!");
        }

        Address input = new Address();
        input.setFullName(form.getFullName());
        input.setPhone(form.getPhone());
        input.setStreet(form.getStreet());
        input.setCity(form.getCity());
        input.setDistrict(form.getDistrict());

        return addressService.createAddressForOrder(user, input, Boolean.TRUE.equals(form.getSaveAsDefault()));
    }

    @PostMapping("/checkout")
    public String placeOrder(@ModelAttribute("checkoutForm") CustomerCheckoutFormDTO form,
                             Authentication auth,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(auth);
        String voucherCode = form.getVoucherCode();
        String paymentMethod = form.getPaymentMethod();

        // MỚI: tính finalPrice đã trừ voucher (nếu có) TRƯỚC khi tính
        // grandTotal — dùng chung cho cả check ngưỡng COD lẫn số tiền gửi
        // sang VNPay. Trước đây 2 chỗ này dùng totalPrice thô (chưa trừ
        // voucher, chưa cộng ship đúng lúc) nên: (1) đơn "hàng đúng 1tr +
        // 30k ship" vẫn lọt COD, và (2) khách có voucher chọn VNPay bị tính
        // tiền cao hơn số thực sẽ lưu vào order.totalPrice.
        //
        // MỚI: validate voucher TRƯỚC khi resolveShippingAddress() — nếu
        // voucher lỗi thì dừng ở đây, tránh lỡ tạo Address mới (trường hợp
        // khách nhập/sửa địa chỉ) rồi không dùng vào đơn nào cả.
        BigDecimal totalPriceBD = BigDecimal.valueOf(cartService.getTotalPrice(user));
        BigDecimal finalPrice = totalPriceBD;
        if (voucherCode != null && !voucherCode.isBlank()) {
            boolean hasSaleItem = cartService.getCartItems(user).stream()
                    .anyMatch(item -> productSaleService
                            .getActiveSale(item.getVariant().getProduct())
                            .isPresent());
            try {
                Voucher voucher = voucherService.applyVoucher(voucherCode, totalPriceBD, user, hasSaleItem);
                finalPrice = voucherService.calcDiscountedPrice(totalPriceBD, voucher);
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/order/checkout";
            }
        }
        Address shippingAddress;
        try {
            shippingAddress = resolveShippingAddress(form, user);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/order/checkout";
        }
        // Không tách billing address riêng nữa — đồng bộ với checkout-guest
        // (billingAddress = shippingAddress), giảm phức tạp không cần thiết
        // cho phần đã đủ việc phải làm trước deadline.
        Address billingAddress = shippingAddress;

        // MỚI (4.5): SỬA LỖI — trước đây grandTotal tính TRƯỚC khi có
        // shippingAddress (dùng flat SHIPPING_FEE), nên số tiền gửi sang
        // VNPay không khớp phí ship thật OrderService.placeOrder() sẽ tính
        // lại lúc callback (dùng chính shippingAddress này). Giờ tính SAU
        // khi đã resolve xong địa chỉ, dùng đúng 1 nguồn logic
        // (OrderService.calculateShippingFee) cho cả khâu thu tiền lẫn lưu đơn.
        int weightGrams = orderService.getCartWeightGrams(user);
        BigDecimal shippingFee = orderService.calculateShippingFee(shippingAddress, finalPrice, weightGrams);
        long grandTotal = finalPrice.add(shippingFee).longValue();

        if ("VNPAY".equals(paymentMethod)) {
            // Lưu vào session để dùng sau khi VNPay callback — shippingAddress
            // ở đây đã được resolve/tạo xong ở trên (có id thật), kể cả
            // trường hợp là địa chỉ "dùng 1 lần" (user = null).
            HttpSession session = request.getSession();
            session.setAttribute("pendingShippingAddressId", shippingAddress.getId());
            session.setAttribute("pendingBillingAddressId", shippingAddress.getId());
            session.setAttribute("pendingVoucherCode", voucherCode);

            // Số tiền thanh toán qua VNPay phải gồm cả phí ship (và giờ đã
            // trừ đúng voucher — xem finalPrice ở trên)
            String paymentUrl = vnPayService.createPaymentUrl(
                    grandTotal,
                    "Thanh toan don hang",
                    request
            );
            return "redirect:" + paymentUrl;
        }

        // COD — tạo order bình thường. Rule ">1 triệu bắt buộc VNPay" giờ
        // nằm trong OrderService.placeOrder() (check bằng grandTotal thật,
        // sau voucher + ship) — bắt exception ở đây để hiển thị lỗi cho khách
        // thay vì để lộ lỗi 500. shippingAddress/billingAddress đã resolve
        // xong ở trên.
        try {
            AppOrder order = orderService.placeOrder(user, shippingAddress,
                    billingAddress, voucherCode, "COD");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công! Mã đơn hàng: #" + order.getOrderCode());
            return "redirect:/order/success/" + order.getId();
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/order/checkout";
        }
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
        // MỚI (4.5): chưa có địa chỉ nào lúc load trang lần đầu (form trống)
        // nên chỉ hiện ước tính mặc định — JS gọi /order/shipping-fee-preview
        // ngay khi khách chọn xong Tỉnh/Quận-Huyện/Phường-Xã (xem checkout-guest.html).
        BigDecimal shippingFee = OrderService.DEFAULT_SHIPPING_FEE;
        BigDecimal grandTotal = totalPrice.add(shippingFee);

        // MỚI: guest không dùng voucher nữa — bỏ hẳn eligibleVouchers,
        // không hiển thị danh sách voucher trên trang checkout-guest.

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("grandTotal", grandTotal);
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

        // MỚI (4.5): tính phí thật từ mã GHN trong form (khách đã chọn xong
        // dropdown trước khi bấm đặt hàng) — cùng logic placeOrderGuest() sẽ
        // dùng lại, tránh lệch số giữa lúc check COD/gửi VNPay và lúc lưu đơn.
        BigDecimal shippingFee = orderService.calculateShippingFeeForGuestForm(form, cartItems, totalPrice);

        // MỚI: guest không dùng voucher nữa — bỏ hẳn finalPrice/voucher,
        // grandTotal chỉ còn totalPrice (đã tự áp sale, nếu có) + ship.
        BigDecimal grandTotal = totalPrice.add(shippingFee);

        // Validate COD > 1 triệu — dùng grandTotal đã cộng ship.
        if (grandTotal.compareTo(BigDecimal.valueOf(1_000_000)) > 0
                && "COD".equals(form.getPaymentMethod())) {
            bindingResult.reject("codLimitExceeded",
                    "Đơn hàng trên 1.000.000 đ (đã gồm phí ship) bắt buộc thanh toán qua VNPay!");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("grandTotal", grandTotal);
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

        // COD — tạo order bình thường. Rule ">1 triệu" đã check ở trên bằng
        // bindingResult, nhưng OrderService.placeOrderGuest cũng tự check lại
        // (phòng hờ nơi khác gọi thẳng service không qua controller này) —
        // nên vẫn bọc try/catch để không lộ lỗi 500 nếu rơi vào edge case.
        try {
            AppOrder order = orderService.placeOrderGuest(form, cartItems);
            guestCartService.clearCart(session);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công! Mã đơn hàng: #" + order.getOrderCode());
            return "redirect:/order/success-guest/" + order.getOrderCode();
        } catch (IllegalStateException | IllegalArgumentException e) {
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", totalPrice);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("view", "shop/checkout-guest");
            return "layouts/shop-layout";
        }
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

            // MỚI: dùng getAddressByIdForOrder() thay vì getAddressById(id, user)
            // — address ở đây đã được xác thực quyền sở hữu (nếu có) lúc resolve
            // ở POST /checkout, và có thể là loại "dùng 1 lần" (user = null) nên
            // getAddressById() thường sẽ NullPointerException khi check ownership.
            Address shippingAddress = addressService.getAddressByIdForOrder(shippingAddressId);
            Address billingAddress  = addressService.getAddressByIdForOrder(billingAddressId);

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

    // MỚI (4.5): AJAX tính phí ship real-time khi khách đổi Tỉnh/Quận-Huyện/
    // Phường-Xã ở dropdown checkout (cả user lẫn guest, phân biệt qua auth).
    // currentTotal do FE tự truyền lên (đã trừ voucher nếu có, xem
    // checkout.html) — chỉ dùng để check ngưỡng freeship, KHÔNG phải nguồn
    // sự thật cuối cùng (đơn hàng thật luôn tính lại totalPrice từ server ở
    // OrderService.placeOrder/placeOrderGuest).
    @GetMapping("/shipping-fee-preview")
    @ResponseBody
    public Map<String, Object> shippingFeePreview(@RequestParam(required = false) Integer ghnDistrictId,
                                                  @RequestParam(required = false) String ghnWardCode,
                                                  @RequestParam BigDecimal currentTotal,
                                                  Authentication auth,
                                                  HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            int weightGrams;
            // ⚠️ GIẢ ĐỊNH: theo cấu hình Spring Security mặc định (AnonymousAuthenticationFilter
            // bật), khách chưa đăng nhập vẫn có Authentication khác null nhưng
            // isAuthenticated() = false hoặc principal = "anonymousUser" — kiểm tra
            // lại nếu SecurityConfig có tùy biến khác cách này.
            boolean isLoggedIn = auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal());
            if (isLoggedIn) {
                AppUser user = getCurrentUser(auth);
                weightGrams = orderService.getCartWeightGrams(user);
            } else {
                List<GuestCartItem> cartItems = guestCartService.getCart(request.getSession());
                weightGrams = orderService.getGuestCartWeightGrams(cartItems);
            }

            Address transientAddress = new Address();
            transientAddress.setGhnDistrictId(ghnDistrictId);
            transientAddress.setGhnWardCode(ghnWardCode);
            BigDecimal fee = orderService.calculateShippingFee(transientAddress, currentTotal, weightGrams);

            result.put("success", true);
            result.put("fee", fee.longValue());
        } catch (Exception e) {
            // Không để lỗi preview chặn checkout — trả về fallback mặc định,
            // phí thật vẫn sẽ được tính đúng lúc submit (placeOrder/placeOrderGuest).
            result.put("success", true);
            result.put("fee", OrderService.DEFAULT_SHIPPING_FEE.longValue());
            result.put("fallback", true);
        }
        return result;
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

            // MỚI (4.4): không cộng dồn sale + voucher — chặn nếu bất kỳ item
            // nào trong giỏ đang có sale active.
            boolean hasSaleItem = cartService.getCartItems(user).stream()
                    .anyMatch(item -> productSaleService
                            .getActiveSale(item.getVariant().getProduct())
                            .isPresent());
            Voucher voucher = voucherService.applyVoucher(code, totalPrice, user, hasSaleItem);
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

        Optional<AppOrder> orderOpt = orderService.lookupGuestOrder(form.getOrderCode());

        if (orderOpt.isEmpty()) {
            orderLookupRateLimiter.recordFailure(rateLimitKey);
            // Thông báo lỗi chung chung khi không tìm thấy mã đơn — xem ghi
            // chú ở OrderService.lookupGuestOrder().
            model.addAttribute("errorMessage", "Không tìm thấy đơn hàng với mã đã nhập.");
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
}