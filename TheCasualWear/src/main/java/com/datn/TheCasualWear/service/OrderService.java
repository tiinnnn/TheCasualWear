package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CancelReasonStatDTO;
import com.datn.TheCasualWear.dto.FrequentCancellerDTO;
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
    private final ProductSaleService        productSaleService; // MỚI: snapshot giá đã áp sale vào order_detail

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

    // Phí ship cố định (fallback an toàn) — sẽ thay bằng GHN API tính phí thực tế ở Giai đoạn 3.
    // public để OrderController tái sử dụng cùng một giá trị (tránh khai báo trùng 2 chỗ).
    public static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(30_000);

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
            voucher              = voucherService.applyVoucher(voucherCode, totalPrice, user);
            BigDecimal discounted = voucherService.calcDiscountedPrice(totalPrice, voucher);
            discountAmount       = totalPrice.subtract(discounted);
            totalPrice           = discounted;
        }

        // Cộng phí ship sau khi đã áp voucher — đây là số tiền cuối cùng khách phải trả
        BigDecimal grandTotal = totalPrice.add(SHIPPING_FEE);

        AppOrder order = new AppOrder();
        order.setCustomer(user);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        // TODO: cần thêm field `shippingFee` (BigDecimal) vào entity AppOrder + migration SQL
        // để lưu lại phí ship của từng đơn (hiện tại đang set tạm, sẽ lỗi compile nếu field chưa có).
        order.setShippingFee(SHIPPING_FEE);
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
        }

        cartService.clearCart(user);

        notificationService.createNotificationForAdmins(
                "Đơn hàng mới #" + order.getId() + " từ khách "
                        + user.getUsername() + " đang chờ xác nhận!",
                "/admin/orders/" + order.getId()
        );
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
    // ADMIN
    // ─────────────────────────────────────────────────────────────

    /**
     * Admin xác nhận đơn PENDING → CONFIRMED. Đây là thời điểm duy nhất đơn
     * online thực sự trừ kho (không trừ lúc đặt hàng nữa) — vì vậy phải
     * validate lại tồn kho ở đây: kho có thể đã đổi kể từ lúc khách đặt
     * (đơn khác được confirm trước, hoặc admin điều chỉnh tay).
     */
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

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được xác nhận! Chúng tôi đang chuẩn bị hàng.",
                "/order/detail/" + orderId
        );
    }

    /**
     * Admin nhập mã vận đơn GHN → chuyển sang SHIPPING.
     * Nhân viên tự tạo đơn trên app GHN rồi copy mã vào đây.
     */
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

    /**
     * Admin tự kiểm tra trạng thái trên GHN (hoặc khách phản hồi qua kênh khác)
     * rồi đánh dấu đơn hoàn thành. Chuyển thẳng SHIPPING → COMPLETED, không còn
     * bước DELIVERED trung gian / không chờ khách xác nhận.
     * COD → đánh dấu đã thu tiền. Doanh thu trên dashboard chỉ tính khi admin
     * chủ động đánh dấu bước này.
     */
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

    /**
     * Dọn đơn CANCELLED và RETURNED sau 1 tháng.
     * Trước đây chỉ query CANCELLED — vì returnOrder() từng gộp chung
     * status CANCELLED cho cả hoàn hàng. Từ khi tách RETURNED riêng,
     * job này phải quét cả 2 status để giữ nguyên hành vi dọn dẹp cũ.
     */
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

    /**
     * Chuẩn hóa khoảng thời gian [from, to] cho các query dashboard (2.4).
     * Quy tắc:
     *  - Không điền gì            → [null, null]      (toàn bộ lịch sử)
     *  - Chỉ điền from            → [from, now()]      (từ from đến hiện tại)
     *  - Chỉ điền to              → [null, to]         (từ đầu đến to)
     *  - Điền cả 2                → [from, to]
     * Dùng chung 1 method này rồi truyền effectiveFrom/effectiveTo vào mọi
     * query, tránh lặp lại if-else ở nhiều nơi. Các query phía repository
     * tự xử lý from/to = null (nghĩa là không giới hạn theo hướng đó).
     */
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

    private void removeOrderVoucher(AppOrder order, Integer orderId) {
        orderVoucherRepository.findByOrderId(orderId).ifPresent(ov -> {
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