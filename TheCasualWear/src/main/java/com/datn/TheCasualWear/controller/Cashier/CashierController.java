package com.datn.TheCasualWear.controller.Cashier;

import com.datn.TheCasualWear.config.VNPayConfig;
import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.pos.PosCart;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.service.CashierService;
import com.datn.TheCasualWear.service.VNPayService;
import com.datn.TheCasualWear.service.VoucherService; // MỚI
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cashier")
public class CashierController {

    private static final String SESSION_CART_IDS_KEY      = "POS_CART_IDS";     // LinkedHashMap<cartId, label>
    private static final String SESSION_ACTIVE_CART_KEY    = "POS_ACTIVE_CART"; // String cartId
    private static final String SESSION_NEXT_CART_NO_KEY   = "POS_NEXT_CART_NO"; // Integer, chỉ tăng, không tái dùng số cũ
    private static final String SESSION_PENDING_VNPAY_KEY  = "PENDING_VNPAY_CHECKOUT";
    private static final int    SEARCH_VARIANTS_PAGE_SIZE  = 20;

    private final CashierService           cashierService;
    private final ProductVariantRepository productVariantRepository;
    private final AppUserRepository        appUserRepository;
    private final VNPayService             vnPayService;
    private final VoucherService           voucherService; // MỚI

    public CashierController(CashierService cashierService,
                             ProductVariantRepository productVariantRepository,
                             AppUserRepository appUserRepository,
                             VNPayService vnPayService,
                             VoucherService voucherService) { // MỚI
        this.cashierService = cashierService;
        this.productVariantRepository = productVariantRepository;
        this.appUserRepository = appUserRepository;
        this.vnPayService = vnPayService;
        this.voucherService = voucherService; // MỚI
    }

    /**
     * Thông tin giao dịch VNPay đang chờ thanh toán, lưu tạm trong session.
     * Chỉ lưu cartId (không copy items) — khi VNPay redirect về
     * /cashier/vnpay-return, mình đọc lại giỏ thật từ registry qua cartId
     * rồi mới gọi CashierService.checkout(). Giỏ trong registry KHÔNG bị
     * đụng tới trong lúc chờ khách quét QR, nên nếu VNPay lỗi/hủy, giỏ vẫn
     * còn nguyên để cashier thử lại hoặc đổi phương thức khác.
     */
    public record PendingVnpayCheckout(
            String txnRef,
            String cartId,
            Integer customerId,
            String voucherCode
    ) implements Serializable {}

    // ─────────────────────────────────────────────────────────────
    // QUẢN LÝ NHIỀU GIỎ TRONG SESSION
    //
    // HttpSession CHỈ giữ danh sách cartId (+ nhãn tab) thuộc về cashier
    // này và cartId đang active. Dữ liệu giỏ thật (items, tồn kho giữ
    // chỗ...) nằm trong PosCartRegistry (in-memory, singleton) — xem
    // package com.datn.TheCasualWear.pos để @Scheduled timeout job quét
    // được across mọi cashier.
    // ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> getCartIds(HttpSession session) {
        Object obj = session.getAttribute(SESSION_CART_IDS_KEY);
        if (obj == null) {
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            session.setAttribute(SESSION_CART_IDS_KEY, map);
            return map;
        }
        return (LinkedHashMap<String, String>) obj;
    }

    /**
     * Số thứ tự cho nhãn "Giỏ N" — chỉ TĂNG DẦN trong suốt phiên làm việc,
     * không bao giờ tái dùng số đã cấp. Nếu chỉ lấy theo cartIds.size()+1
     * (cách cũ) thì đóng tab ở giữa (VD đóng "Giỏ 1" khi đang có "Giỏ 1",
     * "Giỏ 2") sẽ khiến tab mới tạo tiếp theo lại bị đặt trùng tên "Giỏ 2".
     */
    private int nextCartNumber(HttpSession session) {
        Integer next = (Integer) session.getAttribute(SESSION_NEXT_CART_NO_KEY);
        if (next == null) next = 1;
        session.setAttribute(SESSION_NEXT_CART_NO_KEY, next + 1);
        return next;
    }

    /**
     * Trả về cartId đang active, tự dọn các cartId đã "chết" (bị
     * PosCartTimeoutScheduler release) khỏi danh sách, và tự tạo 1 giỏ mới
     * nếu cashier chưa có giỏ nào (lần đầu vào POS, hoặc vừa đóng hết tab).
     */
    private String resolveActiveCartId(HttpSession session) {
        LinkedHashMap<String, String> cartIds = getCartIds(session);

        // Dọn các cartId không còn tồn tại trong registry (đã bị timeout release)
        cartIds.keySet().removeIf(id -> cashierService.getCart(id) == null);

        String activeId = (String) session.getAttribute(SESSION_ACTIVE_CART_KEY);
        if (activeId != null && !cartIds.containsKey(activeId)) {
            activeId = null;
        }

        if (activeId == null) {
            if (!cartIds.isEmpty()) {
                activeId = cartIds.keySet().iterator().next();
            } else {
                PosCart cart = cashierService.createCart("Giỏ " + nextCartNumber(session));
                cartIds.put(cart.getCartId(), cart.getLabel());
                activeId = cart.getCartId();
            }
        }
        session.setAttribute(SESSION_ACTIVE_CART_KEY, activeId);
        return activeId;
    }

    // ─────────────────────────────────────────────────────────────
    // TRANG BÁN HÀNG CHÍNH
    // ─────────────────────────────────────────────────────────────

    @GetMapping
    public String posPage(Model model, HttpSession session) {
        String activeCartId = resolveActiveCartId(session);
        PosCart cart = cashierService.getCartOrThrow(activeCartId);
        LinkedHashMap<String, String> cartIds = getCartIds(session);

        List<Map<String, Object>> cartTabs = cartIds.entrySet().stream()
                .map(e -> {
                    Map<String, Object> tab = new LinkedHashMap<>();
                    tab.put("cartId", e.getKey());
                    tab.put("label", e.getValue());
                    PosCart c = cashierService.getCart(e.getKey());
                    tab.put("itemCount", c != null ? c.getItems().size() : 0);
                    return tab;
                })
                .toList();

        model.addAttribute("cartItems", cart.getItemList());
        model.addAttribute("grandTotal", cart.getGrandTotal());
        model.addAttribute("activeCartId", activeCartId);
        model.addAttribute("cartTabs", cartTabs);
        model.addAttribute("activeVouchers", voucherService.getActiveVouchers()); // MỚI
        model.addAttribute("view", "cashier/pos");
        return "layouts/cashier-layout";
    }

    // ─────────────────────────────────────────────────────────────
    // TAB GIỎ HÀNG — tạo mới / chuyển / đóng
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/cart/new")
    public String newCart(HttpSession session) {
        LinkedHashMap<String, String> cartIds = getCartIds(session);
        String label = "Giỏ " + nextCartNumber(session);
        PosCart cart = cashierService.createCart(label);
        cartIds.put(cart.getCartId(), label);
        session.setAttribute(SESSION_ACTIVE_CART_KEY, cart.getCartId());
        return "redirect:/cashier";
    }

    @PostMapping("/cart/switch")
    public String switchCart(@RequestParam String cartId, HttpSession session) {
        LinkedHashMap<String, String> cartIds = getCartIds(session);
        if (cartIds.containsKey(cartId) && cashierService.getCart(cartId) != null) {
            session.setAttribute(SESSION_ACTIVE_CART_KEY, cartId);
        }
        return "redirect:/cashier";
    }

    // Đóng hẳn 1 tab giỏ — hoàn kho toàn bộ item còn lại trong giỏ đó
    // (khác "Xóa hết" chỉ làm rỗng giỏ nhưng vẫn giữ tab).
    @PostMapping("/cart/close")
    public String closeCart(@RequestParam String cartId, HttpSession session, RedirectAttributes ra) {
        try {
            cashierService.closeCart(cartId);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        LinkedHashMap<String, String> cartIds = getCartIds(session);
        cartIds.remove(cartId);
        if (cartId.equals(session.getAttribute(SESSION_ACTIVE_CART_KEY))) {
            session.removeAttribute(SESSION_ACTIVE_CART_KEY); // resolveActiveCartId sẽ tự chọn/tạo lại
        }
        return "redirect:/cashier";
    }

    // ─────────────────────────────────────────────────────────────
    // TÌM SẢN PHẨM (autocomplete)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/search-variants")
    @ResponseBody
    public List<VariantSearchResultDTO> searchVariants(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {

        Pageable pageable = PageRequest.of(page, SEARCH_VARIANTS_PAGE_SIZE, Sort.by("id").descending());

        // Keyword rỗng/null → hiển thị toàn bộ sản phẩm còn hàng (có phân trang)
        // thay vì trả list rỗng, để mặc định mở POS đã thấy sẵn sản phẩm.
        Page<ProductVariant> variants =
                (keyword == null || keyword.trim().isEmpty())
                        ? productVariantRepository.findAllAvailable(pageable)
                        : productVariantRepository.searchAvailableByKeyword(keyword.trim(), pageable);

        return variants
                .stream()
                .map(v -> {
                    var sale = cashierService.getActiveSale(v);
                    return new VariantSearchResultDTO(
                            v.getId(),
                            v.getProduct().getName(),
                            v.getSize() != null ? v.getSize().getName() : null,
                            v.getColor() != null ? v.getColor().getName() : null,
                            v.getSku(),
                            cashierService.getEffectivePrice(v), // MỚI: giá đã áp sale, để khớp giá lúc thêm vào giỏ
                            v.getStock(),
                            cashierService.resolveImageUrl(v),
                            sale != null ? v.getProduct().getPrice() : null,
                            sale != null ? sale.getDiscountPercent() : null
                    );
                })
                .toList();
    }

    public record VariantSearchResultDTO(
            Integer variantId,
            String productName,
            String sizeName,
            String colorName,
            String sku,
            BigDecimal price,
            Integer stock,
            String imageUrl,
            BigDecimal originalPrice,   // MỚI: giá gốc — null nếu không có sale
            BigDecimal discountPercent  // MỚI: % giảm — null nếu không có sale
    ) {}

    // ─────────────────────────────────────────────────────────────
    // TÌM KHÁCH HÀNG THEO TÊN / EMAIL / SĐT (autocomplete)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/search-customers")
    @ResponseBody
    public List<CustomerSearchResultDTO> searchCustomers(@RequestParam String keyword) {
        return appUserRepository.searchCustomersByKeyword(keyword)
                .stream()
                .map(u -> new CustomerSearchResultDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getPhone(),
                        u.getEmail()
                ))
                .toList();
    }

    public record CustomerSearchResultDTO(
            Integer id,
            String username,
            String phone,
            String email
    ) {}

    // ─────────────────────────────────────────────────────────────
    // VALIDATE VOUCHER (AJAX preview — không trừ lượt dùng)
    //
    // Gọi trước khi submit để hiển thị số tiền được giảm ngay trên màn hình.
    // orderTotal được tính ở client từ giỏ tạm hiện tại.
    // customerId bắt buộc có giá trị — khách vãng lai không được dùng voucher.
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/validate-voucher")
    @ResponseBody
    public CashierService.VoucherPreviewDTO validateVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal,
            @RequestParam(required = false) Integer customerId) {
        return cashierService.validateVoucher(code, orderTotal, customerId);
    }

    // ─────────────────────────────────────────────────────────────
    // THÊM / SỬA SỐ LƯỢNG / XÓA / XÓA HẾT GIỎ TẠM
    // Trừ/hoàn kho ngay lập tức — xem CashierService.addItemToCart() etc.
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Integer variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam String cartId,
                            RedirectAttributes ra) {
        try {
            cashierService.addItemToCart(cartId, variantId, quantity);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    @PostMapping("/cart/update")
    public String updateCartQuantity(@RequestParam Integer variantId,
                                     @RequestParam Integer quantity,
                                     @RequestParam String cartId,
                                     RedirectAttributes ra) {
        try {
            cashierService.setItemQuantity(cartId, variantId, quantity);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Integer variantId,
                                 @RequestParam String cartId,
                                 RedirectAttributes ra) {
        try {
            cashierService.removeItemFromCart(cartId, variantId);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    @PostMapping("/cart/clear")
    public String clearCart(@RequestParam String cartId, RedirectAttributes ra) {
        try {
            cashierService.clearCartItems(cartId);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier";
    }

    // ─────────────────────────────────────────────────────────────
    // THANH TOÁN VNPAY (QR) — bước 1: tạo payment URL, KHÔNG đụng kho
    // (đã giữ chỗ từ lúc add-to-cart). Giỏ vẫn nằm nguyên trong registry,
    // chỉ txnRef + cartId + customerId + voucherCode được lưu tạm trong
    // session (khớp bằng txnRef) để khi VNPay redirect về /vnpay-return
    // mới thực sự tạo đơn.
    //
    // Frontend: gọi endpoint này bằng fetch/AJAX, nhận về payUrl, rồi
    // điều hướng cả trang sang VNPay — trang đó tự hiển thị mã QR để
    // khách quét bằng app ngân hàng.
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/checkout/vnpay/init")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initVnpayCheckout(
            @RequestParam String cartId,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String voucherCode,
            HttpSession session,
            HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        try {
            PosCart cart = cashierService.getCartOrThrow(cartId);
            if (cart.isEmpty()) {
                body.put("error", "Giỏ hàng đang trống!");
                return ResponseEntity.badRequest().body(body);
            }

            BigDecimal total = cashierService.previewCartTotal(cart.getItemList(), voucherCode, customerId);
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                body.put("error", "Số tiền thanh toán không hợp lệ!");
                return ResponseEntity.badRequest().body(body);
            }

            // txnRef duy nhất cho giao dịch này (VNPay yêu cầu không trùng trong ngày)
            String txnRef = System.currentTimeMillis() + "" + VNPayConfig.getRandomNumber(4);

            String returnUrl = UriComponentsBuilder
                    .fromUriString(request.getRequestURL().toString())
                    .replacePath(request.getContextPath() + "/cashier/vnpay-return")
                    .replaceQuery(null)
                    .toUriString();

            String payUrl = vnPayService.createPaymentUrl(
                    total.longValue(),
                    "Thanh toan don POS " + txnRef,
                    txnRef,
                    returnUrl,
                    request);

            session.setAttribute(SESSION_PENDING_VNPAY_KEY,
                    new PendingVnpayCheckout(txnRef, cartId, customerId, voucherCode));

            body.put("payUrl", payUrl);
            body.put("txnRef", txnRef);
            body.put("amount", total);
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            body.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // THANH TOÁN VNPAY (QR) — bước 2: VNPay redirect về đây sau khi
    // khách quét QR và thanh toán xong. Xác thực chữ ký + đối chiếu
    // txnRef với giao dịch đang chờ trong session, rồi mới thực sự
    // tạo AppOrder (dùng lại CashierService.checkout — không trừ kho
    // thêm, đã giữ chỗ từ lúc add-to-cart).
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, HttpSession session, RedirectAttributes ra) {
        Object pendingObj = session.getAttribute(SESSION_PENDING_VNPAY_KEY);

        if (!(pendingObj instanceof PendingVnpayCheckout pending)) {
            ra.addFlashAttribute("errorMessage",
                    "Không tìm thấy giao dịch VNPay đang chờ (có thể phiên làm việc đã hết hạn).");
            return "redirect:/cashier";
        }

        String txnRefFromVnpay = request.getParameter("vnp_TxnRef");
        boolean signatureValid = vnPayService.validateReturn(request);

        if (!signatureValid || txnRefFromVnpay == null || !txnRefFromVnpay.equals(pending.txnRef())) {
            session.removeAttribute(SESSION_PENDING_VNPAY_KEY);
            ra.addFlashAttribute("errorMessage",
                    "Thanh toán VNPay không thành công hoặc chữ ký không hợp lệ.");
            return "redirect:/cashier";
        }

        try {
            AppOrder order = cashierService.checkout(
                    pending.cartId(), pending.customerId(), "VNPAY", pending.voucherCode());
            session.removeAttribute(SESSION_PENDING_VNPAY_KEY);
            forgetCart(session, pending.cartId());
            return "redirect:/cashier/invoice/" + order.getId();
        } catch (Exception e) {
            session.removeAttribute(SESSION_PENDING_VNPAY_KEY);
            ra.addFlashAttribute("errorMessage",
                    "Thanh toán VNPay đã thành công nhưng tạo đơn thất bại: " + e.getMessage()
                            + " — vui lòng liên hệ quản trị viên đối soát giao dịch VNPay txnRef=" + pending.txnRef());
            return "redirect:/cashier";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // THANH TOÁN — tạo AppOrder từ giỏ (đã trừ kho từ lúc add-to-cart)
    // voucherCode chỉ có tác dụng khi customerId != null (check lại trong service)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/checkout")
    public String checkout(@RequestParam String cartId,
                           @RequestParam(required = false) Integer customerId,
                           @RequestParam String paymentMethod,
                           @RequestParam(required = false) String voucherCode,
                           HttpSession session,
                           RedirectAttributes ra) {
        try {
            AppOrder order = cashierService.checkout(cartId, customerId, paymentMethod, voucherCode);
            forgetCart(session, cartId);
            return "redirect:/cashier/invoice/" + order.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier";
        }
    }

    /** Sau khi checkout thành công: bỏ cartId khỏi danh sách tab + active của session. */
    private void forgetCart(HttpSession session, String cartId) {
        getCartIds(session).remove(cartId);
        if (cartId.equals(session.getAttribute(SESSION_ACTIVE_CART_KEY))) {
            session.removeAttribute(SESSION_ACTIVE_CART_KEY);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HÓA ĐƠN — trang in (window.print())
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/invoice/{orderId}")
    public String invoice(@PathVariable Integer orderId, Model model) {
        model.addAttribute("order", cashierService.getOrderForInvoice(orderId));
        model.addAttribute("cancelReasons", CancelReason.values());
        return "cashier/invoice";
    }

    // ─────────────────────────────────────────────────────────────
    // DANH SÁCH ĐƠN ĐÃ BÁN TẠI QUẦY
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String myRecentOrders(@RequestParam(defaultValue = "3") int days, Model model) {
        model.addAttribute("orders", cashierService.getRecentOrdersByCurrentCashier(days));
        model.addAttribute("days", days);
        model.addAttribute("view", "cashier/orders");
        return "layouts/cashier-layout";
    }

    @GetMapping("/orders/{id}")
    public String myOrderDetail(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            AppOrder order = cashierService.getOwnOrderDetail(id);
            model.addAttribute("order", order);
            model.addAttribute("cancelReasons", CancelReason.values());
            model.addAttribute("isAdminOrOwner", cashierService.isCurrentUserAdminOrOwner());
            model.addAttribute("orderPastCancelWindow", cashierService.isOrderPastCancelWindow(order));
            model.addAttribute("cancelWindowMinutes", CashierService.CANCEL_WINDOW_MINUTES);
            model.addAttribute("view", "cashier/order-detail");
            return "layouts/cashier-layout";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier/orders";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HỦY ĐƠN POS — khách đổi ý không thanh toán / tạo nhầm.
    // Dùng chung cho nút "Hủy đơn" ở cả trang hóa đơn (invoice) lẫn
    // trang chi tiết đơn (order-detail) trong danh sách "Đơn đã bán".
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              @RequestParam CancelReason reason,
                              @RequestParam(required = false) String note,
                              RedirectAttributes ra) {
        try {
            cashierService.cancelOrder(id, reason, note);
            ra.addFlashAttribute("successMessage",
                    "Đã hủy đơn hàng #" + id + " và hoàn kho thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cashier/orders/" + id;
    }
}