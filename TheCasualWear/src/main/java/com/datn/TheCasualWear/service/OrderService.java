package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CancelReasonStatDTO;
import com.datn.TheCasualWear.dto.FrequentCancellerDTO;
import com.datn.TheCasualWear.dto.GuestCartItem;
import com.datn.TheCasualWear.dto.GuestCheckoutFormDTO;
import com.datn.TheCasualWear.dto.OrderListDTO;
import com.datn.TheCasualWear.dto.RevenueByChannelDTO;
import com.datn.TheCasualWear.dto.RevenueSummaryDTO;
import org.springframework.data.domain.PageImpl;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import com.datn.TheCasualWear.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AppOrderRepository        orderRepository;
    private final OrderDetailRepository     orderDetailRepository;
    private final OrderVoucherRepository    orderVoucherRepository;
    private final CartService               cartService;
    private final VoucherService            voucherService;
    private final NotificationService       notificationService;
    private final StockMovementLogService   stockMovementLogService;
    private final AppUserRepository         appUserRepository;
    private final ProductSaleService        productSaleService;
    private final OrderEmailService         orderEmailService;
    private final AddressRepository         addressRepository;
    private final ProductVariantRepository  productVariantRepository;
    // MỚI (4.5): tính phí ship thật qua GHN, fallback region-based khi lỗi/thiếu mã GHN
    private final GhnService                ghnService;

    // Lấy admin/owner đang đăng nhập cho các thao tác phía admin (cancelOrderByAdmin,
    // returnOrder). Trả null nếu không xác định được thay vì throw, vì các method
    // này cũng có thể được gọi từ job nền trong tương lai.
    private AppUser getCurrentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            return null;
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElse(null);
    }

    private static final int ADMIN_PAGE_SIZE = 10;
    private static final int RETURN_DAYS     = 15;
    // Dùng khi tính weight mà variantMap thiếu item (hiếm, dữ liệu race
    // condition) — tránh throw ở bước tính phí, lỗi "không tồn tại" thật sự
    // sẽ bị bắt đúng chỗ ở loop tạo OrderDetail bên dưới.
    private static final int DEFAULT_ITEM_WEIGHT_FALLBACK = 300;

    // MỚI (4.5): SHIPPING_FEE cũ (flat 30k) đã bị thay bằng calculateShippingFee()
    // bên dưới — gọi GHN thật, fallback về các hằng số region-based này khi GHN
    // lỗi/timeout hoặc địa chỉ thiếu mã GHN (ghnDistrictId/ghnWardCode null).
    // ⚠️ Vẫn giữ tên "SHIPPING_FEE" ở OrderController cho phần hiển thị giá ước
    // tính TRƯỚC khi có địa chỉ cụ thể (xem OrderController) — dùng DEFAULT_SHIPPING_FEE.
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(1_000_000);
    public static final BigDecimal DEFAULT_SHIPPING_FEE     = BigDecimal.valueOf(30_000); // các tỉnh khác
    private static final BigDecimal HANOI_SHIPPING_FEE      = BigDecimal.valueOf(20_000);
    private static final BigDecimal DANANG_SHIPPING_FEE     = BigDecimal.valueOf(32_000);
    private static final BigDecimal HCM_SHIPPING_FEE        = BigDecimal.valueOf(38_000);

    /**
     * Tính phí ship cuối cùng cho 1 đơn hàng — ưu tiên gọi GHN thật nếu địa
     * chỉ có đủ mã GHN (ghnDistrictId + ghnWardCode), fallback về bảng phí
     * region-based (theo city String) khi: thiếu mã GHN, GHN lỗi/timeout,
     * hoặc weightGrams không hợp lệ. KHÔNG bao giờ throw ra ngoài — checkout
     * phải luôn tính được 1 mức phí nào đó.
     *
     * public: OrderController gọi trực tiếp cho endpoint AJAX
     * /order/shipping-fee-preview (tính lại phí khi khách đổi địa chỉ ở
     * checkout, trước khi bấm đặt hàng — xem checkout.html).
     */
    public BigDecimal calculateShippingFee(Address address, BigDecimal totalPriceAfterDiscount, int weightGrams) {
        if (totalPriceAfterDiscount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        if (address.getGhnDistrictId() != null
                && address.getGhnWardCode() != null && !address.getGhnWardCode().isBlank()) {
            try {
                return ghnService.calculateFee(address.getGhnDistrictId(), address.getGhnWardCode(), weightGrams);
            } catch (GhnApiException e) {
                // Không chặn checkout vì GHN lỗi — rơi xuống fallback region-based bên dưới.
            }
        }

        return calculateShippingFeeRegionBased(address);
    }

    /**
     * Tính phí ship cho guest NGAY LÚC submit form (trước khi tạo Address
     * thật trong DB) — dùng transient Address chỉ để truyền vào
     * calculateShippingFee(), không save. Cần thiết vì OrderController phải
     * biết đúng phí ship TRƯỚC khi biết chấp nhận COD hay không (rule >1tr)
     * và trước khi gửi số tiền sang VNPay — cùng 1 logic
     * placeOrderGuest() sẽ dùng lại khi thực sự tạo đơn.
     */
    public BigDecimal calculateShippingFeeForGuestForm(GuestCheckoutFormDTO form, List<GuestCartItem> cartItems,
                                                       BigDecimal totalPrice) {
        Address transientAddress = new Address();
        transientAddress.setCity(form.getCity());
        transientAddress.setDistrict(form.getDistrict());
        transientAddress.setGhnDistrictId(form.getGhnDistrictId());
        transientAddress.setGhnWardCode(form.getGhnWardCode());
        int weightGrams = getGuestCartWeightGrams(cartItems);
        return calculateShippingFee(transientAddress, totalPrice, weightGrams);
    }

    /** Tổng weight (gram) giỏ hàng của user đã đăng nhập — dùng cho AJAX preview phí ship. */
    public int getCartWeightGrams(AppUser user) {
        return cartService.getCartItems(user).stream()
                .mapToInt(item -> item.getVariant().getProduct().getWeight() * item.getQuantity())
                .sum();
    }

    /** Tổng weight (gram) giỏ hàng guest (session) — dùng cho AJAX preview phí ship. */
    public int getGuestCartWeightGrams(List<GuestCartItem> cartItems) {
        List<Integer> variantIds = cartItems.stream().map(GuestCartItem::getVariantId).toList();
        Map<Integer, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        return cartItems.stream()
                .mapToInt(item -> {
                    ProductVariant v = variantMap.get(item.getVariantId());
                    int unitWeight = (v != null) ? v.getProduct().getWeight() : DEFAULT_ITEM_WEIGHT_FALLBACK;
                    return unitWeight * item.getQuantity();
                })
                .sum();
    }

    private BigDecimal calculateShippingFeeRegionBased(Address address) {
        String city = address.getCity() == null ? "" : address.getCity().toLowerCase();
        if (city.contains("hà nội") || city.contains("ha noi")) {
            return HANOI_SHIPPING_FEE;
        }
        if (city.contains("đà nẵng") || city.contains("da nang")) {
            return DANANG_SHIPPING_FEE;
        }
        if (city.contains("hồ chí minh") || city.contains("ho chi minh")
                || city.contains("tp.hcm") || city.contains("tphcm")) {
            return HCM_SHIPPING_FEE;
        }
        return DEFAULT_SHIPPING_FEE;
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────

    public Page<AppOrder> getAllOrders(String keyword, String status,
                                       String fromDate, String toDate, int page) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        OrderStatus statusEnum = (status == null || status.isBlank())
                ? null : OrderStatus.valueOf(status);
        LocalDateTime from = (fromDate == null || fromDate.isBlank())
                ? null : LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime to = (toDate == null || toDate.isBlank())
                ? null : LocalDate.parse(toDate).atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE);
        return orderRepository.searchOrders(kw, statusEnum, from, to, pageable);
    }

    public Page<OrderListDTO> getOrderDTOs(String keyword, String status,
                                           String fromDate, String toDate, int page) {
        Page<AppOrder> orderPage = getAllOrders(keyword, status, fromDate, toDate, page);
        List<OrderListDTO> dtos = orderPage.getContent().stream()
                .map(OrderListDTO::new)
                .toList();
        return new PageImpl<>(dtos, orderPage.getPageable(), orderPage.getTotalElements());
    }

    public List<AppOrder> getAllOrders() {
        return orderRepository.findAllOrderedByStatus();
    }

    public List<AppOrder> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public AppOrder getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng với id: " + id));
    }

    /**
     * Lấy đơn hàng ĐÃ LOAD SẴN mọi field cần cho email xác nhận (4.3) —
     * customer, shippingAddress, orderDetails + variant/product/size/color.
     *
     * BẮT BUỘC dùng method riêng này (không dùng getOrderById() thường) khi
     * chuẩn bị dữ liệu cho OrderEmailService.sendOrderConfirmationAsync():
     * method đó chạy trên thread khác (mailTaskExecutor, @Async) — không còn
     * nằm trong Hibernate session của request gốc nữa. Nếu truyền vào 1
     * AppOrder có field LAZY chưa load, thread mail sẽ ăn
     * LazyInitializationException ngay khi cố đọc field đó (order.customer,
     * order.shippingAddress, order.orderDetails, variant.product...).
     * ⚠️ CẦN THÊM vào AppOrderRepository nếu chưa có:
     *   @Query("SELECT o FROM AppOrder o " +
     *          "LEFT JOIN FETCH o.customer " +
     *          "LEFT JOIN FETCH o.shippingAddress " +
     *          "LEFT JOIN FETCH o.orderDetails od " +
     *          "LEFT JOIN FETCH od.variant v " +
     *          "LEFT JOIN FETCH v.product " +
     *          "LEFT JOIN FETCH v.size " +
     *          "LEFT JOIN FETCH v.color " +
     *          "WHERE o.id = :id")
     *   Optional<AppOrder> findByIdWithDetailsForEmail(@Param("id") Integer id);
     */
    @Transactional(readOnly = true)
    public AppOrder getOrderForEmail(Integer id) {
        return orderRepository.findByIdWithDetailsForEmail(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng với id: " + id));
    }

    public AppOrder getOrderByIdAndUser(Integer id, AppUser user) {
        AppOrder order = getOrderById(id);
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }

    public List<AppOrder> getOrdersByUser(AppUser user) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(user.getId());
    }

    // ─────────────────────────────────────────────────────────────
    // CUSTOMER
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AppOrder placeOrder(AppUser user, Address shippingAddress,
                               Address billingAddress,
                               String voucherCode, String paymentMethod) {
        List<CartItem> cartItems = cartService.getCartItems(user);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống!");
        }

        BigDecimal totalPrice = BigDecimal.valueOf(cartService.getTotalPrice(user));

        Voucher voucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            // MỚI (4.4): không cộng dồn sale + voucher — chặn nếu bất kỳ item
            // nào trong giỏ đang có sale active.
            boolean hasSaleItem = cartItems.stream()
                    .anyMatch(item -> productSaleService
                            .getActiveSale(item.getVariant().getProduct())
                            .isPresent());
            voucher              = voucherService.applyVoucher(voucherCode, totalPrice, user, hasSaleItem);
            BigDecimal discounted = voucherService.calcDiscountedPrice(totalPrice, voucher);
            discountAmount       = totalPrice.subtract(discounted);
            totalPrice           = discounted;
        }

        // Cộng phí ship sau khi đã áp voucher — đây là số tiền cuối cùng khách phải trả.
        // MỚI (4.5): weight lấy từ CartItem.variant.product (đã fetch sẵn, không query thêm).
        int totalWeightGrams = cartItems.stream()
                .mapToInt(item -> item.getVariant().getProduct().getWeight() * item.getQuantity())
                .sum();
        BigDecimal shippingFee = calculateShippingFee(shippingAddress, totalPrice, totalWeightGrams);
        BigDecimal grandTotal = totalPrice.add(shippingFee);

        if ("COD".equals(paymentMethod)
                && grandTotal.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new IllegalStateException(
                    "Đơn hàng trên 1.000.000 đ (đã gồm phí ship) bắt buộc thanh toán qua VNPay!");
        }

        AppOrder order = new AppOrder();
        // MỚI (6.6): order_code giờ NOT NULL — bắt buộc set ở mọi nơi tạo AppOrder,
        // kể cả luồng user đã login, không chỉ luồng guest.
        order.setOrderCode(generateUniqueOrderCode());
        order.setCustomer(user);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setShippingFee(shippingFee);
        order.setTotalPrice(grandTotal);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setIsPaid("VNPAY".equals(paymentMethod));
        orderRepository.save(order);

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            if (variant.getStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm '" + product.getName()
                                + "' (size: " + (variant.getSize()  != null ? variant.getSize().getName()  : "?")
                                + ", màu: "   + (variant.getColor() != null ? variant.getColor().getName() : "?")
                                + ") chỉ còn " + variant.getStock() + " trong kho!");
            }

            // Giá đã áp sale (nếu sản phẩm đang có sale chạy tại thời điểm đặt
            // hàng) — snapshot vào order_detail.price, không đổi kể cả khi sale
            // hết hạn/thay đổi sau đó. originalPrice snapshot song song để admin
            // biết đơn này có mua lúc đang sale hay không.
            BigDecimal originalPrice = product.getPrice();
            BigDecimal unitPrice     = productSaleService.getEffectivePrice(product);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(unitPrice);
            detail.setOriginalPrice(originalPrice);
            orderDetailRepository.save(detail);

            // KHÔNG trừ kho ở đây nữa — đơn online chỉ thực sự trừ kho khi admin
            // xác nhận (confirmOrder), kèm validate lại tồn kho tại thời điểm đó.
            // Việc check variant.getStock() ở trên chỉ là cảnh báo sớm cho khách,
            // không phải giữ chỗ (không lock/reserve stock lúc đặt hàng).
        }

        if (voucher != null) {
            OrderVoucher orderVoucher = new OrderVoucher();
            orderVoucher.setOrder(order);
            orderVoucher.setVoucher(voucher);
            orderVoucher.setCustomer(user);
            orderVoucher.setDiscountAmount(discountAmount);
            orderVoucherRepository.save(orderVoucher);

            // FIX: applyVoucher() ở trên chỉ validate, không tự tăng usedCount
            // (khác với CashierService.checkout() có tăng) — thiếu dòng này khiến
            // voucher dùng qua online không bao giờ tăng usedCount, dẫn tới có thể
            // bị dùng vượt usageLimit vô hạn lần.
            voucherService.incrementUsedCount(voucher);
        }

        cartService.clearCart(user);

        notificationService.createNotificationForAdmins(
                "Đơn hàng mới #" + order.getId() + " từ khách "
                        + user.getUsername() + " đang chờ xác nhận!",
                "/admin/orders/" + order.getId()
        );

        // MỚI (đổi 4.3): gửi email ngay khi đặt hàng (status PENDING), không
        // còn chờ admin confirm. Dùng getOrderForEmail() (JOIN FETCH sẵn
        // customer/shippingAddress/orderDetails/variant...) chứ không truyền
        // thẳng biến `order` ở trên — sendOrderConfirmationAsync() chạy trên
        // thread khác (mailTaskExecutor, @Async) không còn session Hibernate
        // của transaction này, đọc field LAZY sẽ ăn LazyInitializationException.
        // Gọi self (this.getOrderForEmail) nên bỏ qua proxy @Transactional của
        // chính nó, nhưng vẫn chạy được vì đang ở trong transaction của
        // placeOrder() rồi — không sao.
        orderEmailService.sendOrderConfirmationAsync(getOrderForEmail(order.getId()));

        return order;
    }

    @Transactional
    public void cancelOrder(Integer orderId, AppUser user, CancelReason reason, String note) {
        AppOrder order = getOrderByIdAndUser(orderId, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận!");
        }
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            throw new IllegalStateException(
                    "Đơn hàng đã thanh toán không thể hủy trực tiếp. "
                            + "Vui lòng liên hệ Zalo 0901.234.567 để được hỗ trợ!");
        }
        validateCancelReason(reason, note);

        // Đơn đang PENDING chưa từng bị trừ kho (kho chỉ trừ khi admin confirm),
        // nên không cần restoreVariantStock ở đây — restore sẽ làm sai lệch tồn kho.
        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.CANCELLED);
        applyCancelMetadata(order, reason, note, user);
        orderRepository.save(order);

        notificationService.createNotificationForAdmins(
                "Khách hàng " + user.getUsername()
                        + " đã hủy đơn hàng #" + orderId + "!",
                "/admin/orders/" + orderId
        );
        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được hủy!",
                "/order/detail/" + orderId
        );
    }

    // ─────────────────────────────────────────────────────────────
    // CUSTOMER — KHÁCH VÃNG LAI (4.1)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AppOrder placeOrderGuest(GuestCheckoutFormDTO form, List<GuestCartItem> cartItems) {
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống!");
        }

        // Guest luôn tạo Address mới (user = null) — không có địa chỉ đã lưu để chọn,
        // khác addressService.getAddressById(id, user) ở luồng user đã login.
        Address shippingAddress = new Address();
        shippingAddress.setUser(null);
        shippingAddress.setFullName(form.getFullName());
        shippingAddress.setPhone(form.getPhone());
        shippingAddress.setStreet(form.getStreet());
        shippingAddress.setCity(form.getCity());
        shippingAddress.setDistrict(form.getDistrict());
        // MỚI (4.5): mã GHN lấy từ dropdown Quận/Huyện + Phường/Xã theo GHN
        // (khác cascade city/district ở trên) — NULL nếu khách bỏ qua/GHN lỗi
        // lúc load dropdown, calculateShippingFee() sẽ tự fallback region-based.
        shippingAddress.setGhnProvinceId(form.getGhnProvinceId());
        shippingAddress.setGhnDistrictId(form.getGhnDistrictId());
        shippingAddress.setGhnWardCode(form.getGhnWardCode());
        shippingAddress.setIsDefault(false);
        addressRepository.save(shippingAddress);

        BigDecimal totalPrice = cartItems.stream()
                .map(GuestCartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // MỚI (4.5): fetch hết ProductVariant 1 lần (thay vì query lẻ từng item
        // trong loop tạo OrderDetail bên dưới) — dùng lại map này cho cả tính
        // weight lẫn tạo OrderDetail, đỡ N truy vấn trùng lặp.
        List<Integer> variantIds = cartItems.stream().map(GuestCartItem::getVariantId).toList();
        Map<Integer, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        int totalWeightGrams = cartItems.stream()
                .mapToInt(item -> {
                    ProductVariant v = variantMap.get(item.getVariantId());
                    int unitWeight = (v != null) ? v.getProduct().getWeight() : DEFAULT_ITEM_WEIGHT_FALLBACK;
                    return unitWeight * item.getQuantity();
                })
                .sum();

        BigDecimal shippingFee = calculateShippingFee(shippingAddress, totalPrice, totalWeightGrams);
        BigDecimal grandTotal = totalPrice.add(shippingFee);

        if ("COD".equals(form.getPaymentMethod())
                && grandTotal.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new IllegalStateException(
                    "Đơn hàng trên 1.000.000 đ (đã gồm phí ship) bắt buộc thanh toán qua VNPay!");
        }

        AppOrder order = new AppOrder();
        order.setOrderCode(generateUniqueOrderCode());
        order.setCustomer(null); // guest — không gắn AppUser
        order.setGuestEmail(form.getEmail());
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(shippingAddress); // guest không tách địa chỉ thanh toán riêng
        order.setShippingFee(shippingFee);
        order.setTotalPrice(grandTotal);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(form.getPaymentMethod());
        order.setIsPaid(false); // guest hiện chỉ hỗ trợ COD
        orderRepository.save(order);

        for (GuestCartItem item : cartItems) {
            ProductVariant variant = variantMap.get(item.getVariantId());
            if (variant == null) {
                throw new IllegalArgumentException("Sản phẩm không tồn tại");
            }

            if (variant.getStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm '" + item.getProductName() + "' chỉ còn "
                                + variant.getStock() + " trong kho!");
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice());
            detail.setOriginalPrice(item.getOriginalPrice() != null
                    ? item.getOriginalPrice() : item.getUnitPrice());
            orderDetailRepository.save(detail);

        }

        // Không còn lưu OrderVoucher cho guest — guest không dùng voucher nữa.

        notificationService.createNotificationForAdmins(
                "Đơn hàng mới #" + order.getOrderCode() + " từ khách vãng lai ("
                        + form.getPhone() + ") đang chờ xác nhận!",
                "/admin/orders/" + order.getId()
        );

        orderEmailService.sendOrderConfirmationAsync(getOrderForEmail(order.getId()));

        return order;
    }

    /** Tra đơn theo order_code — dùng cho trang thành công của guest (4.1) và tra cứu đơn (4.2/6.6). */
    public AppOrder getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng với mã: " + orderCode));
    }

    // ĐÃ ĐỔI (theo yêu cầu mới): bỏ yêu cầu SĐT/email — khách chỉ cần nhập
    // đúng mã đơn hàng là tra cứu được. Trước đây bắt buộc khớp cả contact
    // (phone HOẶC guestEmail) trong 1 query gộp để chống dò mã đơn hàng, nhưng
    // đây cũng chính là nguyên nhân lỗi "copy đúng mã đơn mà không ra thông
    // tin" — chỉ cần lệch định dạng SĐT/email 1 chút (khoảng trắng, hoa/thường,
    // +84 vs 0...) là query không khớp, dù mã đơn hàng đúng 100%.
    // ⚠️ Đánh đổi bảo mật: giờ chỉ cần biết mã đơn hàng (8 ký tự hex ngẫu
    // nhiên, generateUniqueOrderCode()) là xem được thông tin đơn — không gian
    // mã đủ lớn (36^8) nên khó dò mù, nhưng nếu mã bị lộ (chụp màn hình, gửi
    // qua tin nhắn công khai...) ai cũng tra được. OrderLookupRateLimiter vẫn
    // giữ nguyên để chặn dò mã hàng loạt.
    public java.util.Optional<AppOrder> lookupGuestOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return java.util.Optional.empty();
        }
        return orderRepository.findByOrderCode(orderCode.trim().toUpperCase());
    }

    private String generateUniqueOrderCode() {
        String code;
        do {
            code = java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void confirmOrder(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ xác nhận!");
        }

        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());

        // Validate lại tồn kho tại thời điểm confirm — không cho confirm nếu
        // bất kỳ variant nào không còn đủ hàng.
        for (OrderDetail detail : details) {
            ProductVariant variant = detail.getVariant();
            Integer needed = detail.getQuantity();
            if (variant.getStock() < needed) {
                throw new IllegalStateException(
                        "Sản phẩm '" + variant.getProduct().getName()
                                + "' (size: " + (variant.getSize()  != null ? variant.getSize().getName()  : "?")
                                + ", màu: "   + (variant.getColor() != null ? variant.getColor().getName() : "?")
                                + ") chỉ còn " + variant.getStock()
                                + " trong kho, không đủ để xác nhận đơn (cần " + needed + ")!");
            }
        }

        AppUser actor = getCurrentUserOrNull();
        for (OrderDetail detail : details) {
            stockMovementLogService.logMovement(
                    detail.getVariant(),
                    StockMovementType.SALE,
                    -detail.getQuantity(),
                    StockRefType.ORDER,
                    order.getId(),
                    "Xác nhận đơn online #" + order.getId(),
                    actor
            );
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        if (order.getCustomer() != null) {
            notificationService.createNotification(
                    order.getCustomer(),
                    "Đơn hàng #" + orderId + " đã được xác nhận! Chúng tôi đang chuẩn bị hàng.",
                    "/order/detail/" + orderId
            );
        }

        // MỚI (4.3): gửi email xác nhận đơn — bắt buộc dùng getOrderForEmail()
        // (fetch join sẵn) chứ không phải "order" ở trên, vì
        // sendOrderConfirmationAsync() chạy trên thread khác (@Async) —
        // field LAZY chưa load sẽ ăn LazyInitializationException.
        orderEmailService.sendOrderConfirmationAsync(getOrderForEmail(orderId));
    }

    @Transactional
    public void shipOrder(Integer orderId, String trackingCode) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể gửi hàng khi đơn đã được xác nhận!");
        }
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalStateException("Vui lòng nhập mã vận đơn!");
        }
        order.setTrackingCode(trackingCode.trim());
        order.setStatus(OrderStatus.SHIPPING);
        order.setShippedAt(LocalDateTime.now());
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được gửi đi! Mã vận đơn GHN: "
                        + trackingCode.trim() + ". Bạn có thể tra cứu tại ghn.vn",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void completeOrderByAdmin(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái đang giao!");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setDeliveredAt(LocalDateTime.now());
        if ("COD".equals(order.getPaymentMethod())) {
            order.setIsPaid(true);
        }
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã hoàn thành. Cảm ơn bạn đã mua hàng! =))",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void cancelOrderByAdmin(Integer orderId, CancelReason reason, String note) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.RETURNED) {
            throw new IllegalStateException("Không thể hủy đơn hàng này!");
        }
        // Đã có mã vận đơn GHN (tức đã gửi hàng) -> không cho hủy trực tiếp
        // nữa, kể cả khi request được gửi thẳng qua form (bỏ qua FE).
        if (order.getTrackingCode() != null) {
            throw new IllegalStateException(
                    "Đơn hàng đã có mã vận đơn GHN, không thể hủy trực tiếp. "
                            + "Liên hệ GHN để yêu cầu thu hồi nếu cần.");
        }
        validateCancelReason(reason, note);

        AppUser actor = getCurrentUserOrNull();
        // Chỉ restore kho nếu đơn đã qua confirm (từ CONFIRMED trở lên) — vì giờ
        // đơn online chỉ trừ kho lúc confirmOrder(), đơn còn PENDING chưa từng
        // bị trừ nên không cần (và không được) restore.
        if (order.getStatus() != OrderStatus.PENDING) {
            restoreVariantStock(order, StockMovementType.CANCEL, actor);
        }
        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.CANCELLED);
        applyCancelMetadata(order, reason, note, actor);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã bị hủy bởi shop. Liên hệ hỗ trợ nếu có thắc mắc.",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void returnOrder(Integer orderId, boolean restock, CancelReason reason, String note) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ có thể hoàn hàng khi đơn đã hoàn thành!");
        }
        if (order.getDeliveredAt() == null) {
            throw new IllegalStateException("Không xác định được ngày giao hàng!");
        }

        LocalDateTime deadline = order.getDeliveredAt().plusDays(RETURN_DAYS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException(
                    "Đã quá " + RETURN_DAYS + " ngày kể từ khi giao, không thể hoàn hàng!");
        }
        validateCancelReason(reason, note);

        AppUser actor = getCurrentUserOrNull();
        if (restock) restoreVariantStock(order, StockMovementType.RETURN, actor);

        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.RETURNED);
        applyCancelMetadata(order, reason, note, actor);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được hoàn hàng do sản phẩm lỗi. "
                        + "Shop xin lỗi vì sự bất tiện này!",
                "/order/detail/" + orderId
        );
    }

    // ─────────────────────────────────────────────────────────────
    // SCHEDULED JOBS
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteCancelledOrderAfterMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<AppOrder> toDelete = new ArrayList<>();
        toDelete.addAll(orderRepository.findByStatus(OrderStatus.CANCELLED));
        toDelete.addAll(orderRepository.findByStatus(OrderStatus.RETURNED));
        toDelete.stream()
                .filter(o -> o.getOrderDate().isBefore(oneMonthAgo))
                .forEach(orderRepository::delete);
    }

    // ─────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────

    public DateRange resolveDateRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveTo = (to != null) ? to
                : (from != null ? LocalDateTime.now() : null);
        return new DateRange(from, effectiveTo);
    }

    public record DateRange(LocalDateTime from, LocalDateTime to) {}

    // SUM/COUNT không GROUP BY luôn trả về đúng 1 dòng, nhưng lấy an toàn
    // qua List<Object[]> để phòng trường hợp bất thường (tránh IndexOutOfBounds).
    private Object[] firstRow(List<Object[]> rows) {
        return (rows != null && !rows.isEmpty())
                ? rows.get(0)
                : new Object[]{BigDecimal.ZERO, 0L};
    }

    /**
     * Doanh thu/lợi nhuận theo khoảng thời gian tùy chọn (2.4/2.5).
     * Công thức: doanh thu = POS (COUNTER, COMPLETED)
     *                      + Online VNPay đã thanh toán (chưa hủy/hoàn)
     *                      + Online COD đã hoàn tất giao hàng (COMPLETED)
     *            lợi nhuận = doanh thu - giá vốn (costPrice) của các sản phẩm
     *                        trong đúng tập đơn được tính ở trên.
     */
    public RevenueSummaryDTO getRevenueSummary(LocalDateTime from, LocalDateTime to) {
        DateRange range = resolveDateRange(from, to);
        LocalDateTime effFrom = range.from();
        LocalDateTime effTo   = range.to();

        // Query aggregate (SUM/COUNT không GROUP BY) luôn trả về đúng 1 dòng,
        // nhưng Spring Data JPA yêu cầu khai báo List<Object[]> chứ không phải
        // Object[] trực tiếp — nếu khai Object[] nó sẽ lấy nguyên dòng làm
        // phần tử (gây ClassCastException khi cast pos[0] sang BigDecimal).
        Object[] pos   = firstRow(orderRepository.sumPosRevenue(effFrom, effTo));
        Object[] vnpay = firstRow(orderRepository.sumOnlineVnpayRevenue(effFrom, effTo));
        Object[] cod   = firstRow(orderRepository.sumOnlineCodRevenue(effFrom, effTo));
        BigDecimal cost = orderRepository.sumCostForRevenueOrders(effFrom, effTo);

        BigDecimal posRevenue   = (BigDecimal) pos[0];
        long       posOrders    = (Long) pos[1];
        BigDecimal vnpayRevenue = (BigDecimal) vnpay[0];
        long       vnpayOrders  = (Long) vnpay[1];
        BigDecimal codRevenue   = (BigDecimal) cod[0];
        long       codOrders    = (Long) cod[1];

        BigDecimal totalRevenue = posRevenue.add(vnpayRevenue).add(codRevenue);
        BigDecimal totalCost    = (cost != null) ? cost : BigDecimal.ZERO;
        BigDecimal totalProfit  = totalRevenue.subtract(totalCost);

        return new RevenueSummaryDTO(
                posRevenue, posOrders,
                vnpayRevenue, vnpayOrders,
                codRevenue, codOrders,
                totalRevenue, totalCost, totalProfit
        );
    }

    // Doanh thu ONLINE vs COUNTER trong khoảng thời gian (chỉ đơn COMPLETED)
    public List<RevenueByChannelDTO> getRevenueByChannel(LocalDateTime from, LocalDateTime to) {
        return orderRepository.sumRevenueByOrderType(from, to).stream()
                .map(r -> new RevenueByChannelDTO(
                        (com.datn.TheCasualWear.enums.OrderType) r[0],
                        (BigDecimal) r[1],
                        (Long) r[2]))
                .toList();
    }

    // Thống kê lý do hủy/hoàn đơn, nhiều nhất trước
    public List<CancelReasonStatDTO> getCancelReasonStats() {
        return orderRepository.countByCancelReason().stream()
                .map(r -> new CancelReasonStatDTO((CancelReason) r[0], (Long) r[1]))
                .toList();
    }

    // Khách hàng có >= minCount đơn CANCELLED/RETURNED — chỉ để admin xem xét
    // thủ công, KHÔNG tự động chặn mua hàng (xem ghi chú ở FrequentCancellerDTO).
    public List<FrequentCancellerDTO> getFrequentCancellers(long minCount) {
        return orderRepository.findFrequentCancellers(minCount).stream()
                .map(r -> new FrequentCancellerDTO(
                        (AppUser) r[0], (Long) r[1], (Long) r[2], (Long) r[3]))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    @Transactional
    protected void restoreVariantStock(AppOrder order, StockMovementType movementType, AppUser actor) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            stockMovementLogService.logMovement(
                    detail.getVariant(),
                    movementType,
                    detail.getQuantity(),
                    StockRefType.ORDER,
                    order.getId(),
                    (movementType == StockMovementType.RETURN ? "Hoàn hàng" : "Hủy đơn")
                            + " #" + order.getId(),
                    actor
            );
        }
    }

    // Dùng chung cho cancelOrder (khách), cancelOrderByAdmin, returnOrder.
    // FIX: trước đây chỉ xóa OrderVoucher, không hoàn usedCount — giờ khớp
    // với placeOrder() đã tăng usedCount lúc tạo đơn (và khớp với cách
    // CashierService.cancelOrder() đang hoàn lượt dùng cho đơn quầy).
    private void removeOrderVoucher(AppOrder order, Integer orderId) {
        orderVoucherRepository.findByOrderId(orderId).ifPresent(ov -> {
            voucherService.decrementUsedCount(ov.getVoucher());
            order.setOrderVoucher(null);
            orderVoucherRepository.delete(ov);
        });
    }

    /** reason bắt buộc; nếu chọn OTHER thì note cũng bắt buộc (để còn biết lý do là gì). */
    private void validateCancelReason(CancelReason reason, String note) {
        if (reason == null) {
            throw new IllegalStateException("Vui lòng chọn lý do!");
        }
        if (reason == CancelReason.OTHER && (note == null || note.isBlank())) {
            throw new IllegalStateException("Vui lòng nhập ghi chú khi chọn lý do 'Khác'!");
        }
    }

    private void applyCancelMetadata(AppOrder order, CancelReason reason, String note, AppUser actor) {
        order.setCancelReason(reason);
        order.setCancelNote((note == null || note.isBlank()) ? null : note.trim());
        order.setCancelledBy(actor);
        order.setCancelledAt(LocalDateTime.now());
    }
}