package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
import com.datn.TheCasualWear.enums.StockMovementType;
import com.datn.TheCasualWear.enums.StockRefType;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.OrderDetailRepository;
import com.datn.TheCasualWear.repository.OrderVoucherRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import com.datn.TheCasualWear.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashierService {

    private final AppOrderRepository       orderRepository;
    private final AppUserRepository        appUserRepository;
    private final ProductVariantRepository variantRepository;
    private final VoucherRepository        voucherRepository;
    private final OrderDetailRepository    orderDetailRepository;
    private final OrderVoucherRepository   orderVoucherRepository;
    private final StockMovementLogService  stockMovementLogService;
    private final ProductSaleService       productSaleService; // MỚI: giá sale áp cho cả bán tại quầy
    private final ShiftService       shiftService;

    // Cashier tự hủy đơn trong vòng bao nhiêu phút kể từ lúc tạo.
    // Admin/Owner không bị giới hạn bởi mốc thời gian này.
    private static final int CANCEL_WINDOW_MINUTES = 30;

    // ─────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────

    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User principal)) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập");
        }
        return appUserRepository.findByUsernameOrEmailOrPhone(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    /**
     * Tính số tiền được giảm từ voucher dựa trên tổng đơn hàng.
     * discountPercent% của orderTotal, không vượt quá maxDiscount (nếu có).
     */
    private BigDecimal calcDiscount(Voucher v, BigDecimal orderTotal) {
        BigDecimal discount = orderTotal
                .multiply(v.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) {
            discount = v.getMaxDiscount();
        }
        return discount;
    }

    // ─────────────────────────────────────────────────────────────
    // GIÁ BÁN — đã áp sale nếu sản phẩm đang có sale chạy. Dùng chung
    // cho cả buildCartItem() (thêm vào giỏ) và ô tìm kiếm autocomplete
    // ở CashierController, để giá hiển thị lúc tìm và giá lúc thêm
    // vào giỏ luôn khớp nhau.
    // ─────────────────────────────────────────────────────────────

    public BigDecimal getEffectivePrice(ProductVariant variant) {
        return productSaleService.getEffectivePrice(variant.getProduct());
    }

    // Sale đang chạy của sản phẩm (nếu có) — dùng để hiện badge "-X%" +
    // giá gốc gạch ngang cho cashier, cả lúc tìm kiếm lẫn trong giỏ tạm.
    public ProductSale getActiveSale(ProductVariant variant) {
        return productSaleService.getActiveSale(variant.getProduct()).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // XÂY DỰNG ITEM GIỎ TẠM
    // ─────────────────────────────────────────────────────────────

    public CounterCartItemDTO buildCartItem(Integer variantId, int quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (quantity < 1) {
            throw new IllegalStateException("Số lượng phải lớn hơn 0");
        }
        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                    "Sản phẩm '" + variant.getProduct().getName()
                            + "' chỉ còn " + variant.getStock() + " trong kho!");
        }

        BigDecimal unitPrice = getEffectivePrice(variant);
        ProductSale activeSale = getActiveSale(variant);

        return new CounterCartItemDTO(
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSize() != null ? variant.getSize().getName() : null,
                variant.getColor() != null ? variant.getColor().getName() : null,
                variant.getSku(),
                unitPrice,
                quantity,
                variant.getStock(),
                resolveImageUrl(variant),
                activeSale != null ? variant.getProduct().getPrice() : null,
                activeSale != null ? activeSale.getDiscountPercent() : null
        );
    }

    // ─────────────────────────────────────────────────────────────
    // ẢNH HIỂN THỊ — ưu tiên ảnh riêng của variant (variant_image,
    // theo sortOrder) để phân biệt đúng màu/kiểu; nếu variant chưa có
    // ảnh nào thì tạm dùng ảnh đầu tiên của Product làm ảnh thay thế.
    // Public để CashierController tái sử dụng cho /search-variants.
    // ─────────────────────────────────────────────────────────────

    public String resolveImageUrl(ProductVariant variant) {
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().get(0).getImageUrl();
        }
        Product product = variant.getProduct();
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0).getImageUrl();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATE VOUCHER (preview trước khi checkout — không trừ lượt dùng)
    //
    // Quy tắc bảo vệ:
    //   • Voucher chỉ áp dụng khi khách có tài khoản (customerId != null).
    //     Không cho khách vãng lai dùng voucher để tránh bị lợi dụng.
    //   • Kiểm tra isActive, thời hạn, usageLimit, minOrderValue.
    //   • Trả VoucherPreviewDTO để JS hiển thị số tiền giảm ngay trên màn hình.
    // ─────────────────────────────────────────────────────────────

    public VoucherPreviewDTO validateVoucher(String code,
                                             BigDecimal orderTotal,
                                             Integer customerId) {
        // Voucher chỉ dành cho khách có tài khoản
        if (customerId == null) {
            return VoucherPreviewDTO.fail("Voucher chỉ áp dụng cho khách có tài khoản.");
        }

        Voucher v = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElse(null);
        if (v == null) {
            return VoucherPreviewDTO.fail("Mã voucher không tồn tại.");
        }

        if (Boolean.FALSE.equals(v.getIsActive())) {
            return VoucherPreviewDTO.fail("Mã voucher đã bị vô hiệu hóa.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            return VoucherPreviewDTO.fail("Mã voucher chưa đến ngày áp dụng.");
        }
        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            return VoucherPreviewDTO.fail("Mã voucher đã hết hạn.");
        }

        // Kiểm tra số lượng còn lại
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return VoucherPreviewDTO.fail("Mã voucher đã hết lượt sử dụng.");
        }

        if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) {
            return VoucherPreviewDTO.fail(
                    "Đơn tối thiểu " + v.getMinOrderValue().toPlainString() + " đ để dùng mã này.");
        }

        BigDecimal discount = calcDiscount(v, orderTotal);
        String msg = "Giảm " + v.getDiscountPercent().stripTrailingZeros().toPlainString() + "%";
        if (v.getMaxDiscount() != null) {
            msg += " (tối đa " + v.getMaxDiscount().toPlainString() + " đ)";
        }
        // Còn lại bao nhiêu lượt
        if (v.getUsageLimit() != null) {
            int remaining = v.getUsageLimit() - v.getUsedCount();
            msg += " · Còn " + remaining + " lượt";
        }

        return VoucherPreviewDTO.ok(discount, msg);
    }

    /** DTO trả về cho endpoint /validate-voucher */
    public record VoucherPreviewDTO(boolean valid, String message, BigDecimal discountAmount) {
        static VoucherPreviewDTO ok(BigDecimal discount, String msg) {
            return new VoucherPreviewDTO(true, msg, discount);
        }
        static VoucherPreviewDTO fail(String msg) {
            return new VoucherPreviewDTO(false, msg, BigDecimal.ZERO);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TÍNH TỔNG TIỀN GIỎ HÀNG (dùng để tạo số tiền thanh toán VNPay)
    // Không trừ kho, không trừ lượt voucher — chỉ preview.
    // ─────────────────────────────────────────────────────────────

    public BigDecimal previewCartTotal(List<CounterCartItemDTO> items,
                                       String voucherCode,
                                       Integer customerId) {
        BigDecimal subtotal = items.stream()
                .map(CounterCartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank() && customerId != null) {
            VoucherPreviewDTO preview = validateVoucher(voucherCode, subtotal, customerId);
            if (preview.valid()) {
                discount = preview.discountAmount();
            }
        }
        return subtotal.subtract(discount).max(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────
    // CHECKOUT — trừ kho + áp voucher (nếu có) + tạo AppOrder
    //
    // Toàn bộ nằm trong 1 @Transactional:
    //   trừ kho → validate & trừ usedCount voucher → save order
    // Nếu bất kỳ bước nào fail → rollback toàn bộ, không mất kho / lượt voucher.
    //
    // Giá lưu vào order_detail lấy từ item.getUnitPrice() — đã được
    // buildCartItem() tính theo giá sale ngay khi thêm vào giỏ tạm, nên
    // ở đây không cần tính lại (và cũng không nên: giữ đúng giá lúc
    // khách được báo giá, tránh lệch nếu sale hết hạn giữa lúc thao tác).
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AppOrder checkout(Integer customerId,
                             List<CounterCartItemDTO> items,
                             String paymentMethod,
                             String voucherCode) {

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống!");
        }

        AppUser cashier = getCurrentUser();

        AppOrder order = new AppOrder();
        order.setOrderType(OrderType.COUNTER);
        order.setCashier(cashier);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentMethod(paymentMethod);
        order.setIsPaid(true);
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveredAt(LocalDateTime.now());
        Shift shift = shiftService.getOpenShiftOrThrow(cashier);
        order.setShift(shift);

        if (customerId != null) {
            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
            order.setCustomer(customer);
        }

        // Lưu trước để có order.getId() dùng làm refId khi ghi log biến động kho
        orderRepository.save(order);

        // ── Tính tổng hàng ──────────────────────────────────────
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CounterCartItemDTO item : items) {
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm không tồn tại: " + item.getVariantId()));

            if (variant.getStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm '" + variant.getProduct().getName() + "' chỉ còn "
                                + variant.getStock() + " trong kho (giỏ yêu cầu "
                                + item.getQuantity() + ")!");
            }

            stockMovementLogService.logMovement(
                    variant,
                    StockMovementType.SALE,
                    -item.getQuantity(),
                    StockRefType.ORDER,
                    order.getId(),
                    "Bán tại quầy - đơn #" + order.getId(),
                    cashier
            );

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice());
            detail.setOriginalPrice(item.getOriginalPrice() != null
                    ? item.getOriginalPrice() : item.getUnitPrice());
            order.getOrderDetails().add(detail);

            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // ── Áp voucher (chỉ khi khách có tài khoản + nhập mã) ──
        BigDecimal discount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.isBlank() && customerId != null) {
            // Validate lại lần nữa trong transaction để chống race condition
            // (ví dụ 2 cashier dùng cùng mã cùng lúc ở 2 tab)
            VoucherPreviewDTO preview = validateVoucher(voucherCode, subtotal, customerId);
            if (!preview.valid()) {
                throw new IllegalStateException("Voucher không hợp lệ: " + preview.message());
            }

            Voucher voucher = voucherRepository.findByCode(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalStateException("Voucher không hợp lệ: không tìm thấy mã"));
            discount = preview.discountAmount();
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);

            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

            OrderVoucher orderVoucher = new OrderVoucher();
            orderVoucher.setOrder(order);
            orderVoucher.setVoucher(voucher);
            orderVoucher.setCustomer(customer);
            orderVoucher.setDiscountAmount(discount);

            order.setOrderVoucher(orderVoucher);
        }

        order.setTotalPrice(subtotal.subtract(discount).max(BigDecimal.ZERO));

        return orderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────
    // CÁC METHOD KHÁC (giữ nguyên)
    // ─────────────────────────────────────────────────────────────

    public AppOrder getOrderForInvoice(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    public List<AppOrder> getRecentOrdersByCurrentCashier(int days) {
        AppUser cashier = getCurrentUser();
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return orderRepository.findRecentCounterOrdersByCashier(cashier.getId(), from);
    }

    private boolean isCurrentUserAdminOrOwner() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_OWNER"));
    }

    public AppOrder getOwnOrderDetail(Integer orderId) {
        AppOrder order = getOrderForInvoice(orderId);
        if (isCurrentUserAdminOrOwner()) return order;

        AppUser cashier = getCurrentUser();
        if (order.getCashier() == null || !order.getCashier().getId().equals(cashier.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }

    // ─────────────────────────────────────────────────────────────
    // HỦY ĐƠN POS — khách đổi ý không thanh toán / thu ngân lỡ tạo nhầm.
    //
    // Quy tắc:
    //   • Chỉ hủy được đơn COUNTER, đang ở trạng thái COMPLETED
    //     (đơn POS luôn tạo thẳng COMPLETED, không có trạng thái PENDING).
    //   • Cashier chỉ hủy được đơn của chính mình, trong vòng
    //     CANCEL_WINDOW_MINUTES kể từ lúc tạo. Admin/Owner hủy được
    //     bất kỳ lúc nào (không giới hạn thời gian).
    //   • Hoàn kho từng dòng sản phẩm + hoàn lượt dùng voucher (nếu có).
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void cancelOrder(Integer orderId, CancelReason reason, String note) {
        AppOrder order = getOwnOrderDetail(orderId); // đã kiểm tra quyền sở hữu / admin-owner

        if (order.getOrderType() != OrderType.COUNTER) {
            throw new IllegalStateException("Chỉ có thể hủy đơn bán tại quầy!");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái có thể hủy!");
        }
        if (reason == null) {
            throw new IllegalStateException("Vui lòng chọn lý do hủy!");
        }
        if (reason == CancelReason.OTHER && (note == null || note.isBlank())) {
            throw new IllegalStateException("Vui lòng nhập ghi chú khi chọn lý do 'Khác'!");
        }

        if (!isCurrentUserAdminOrOwner()) {
            LocalDateTime deadline = order.getOrderDate().plusMinutes(CANCEL_WINDOW_MINUTES);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new IllegalStateException(
                        "Đơn hàng đã tạo quá " + CANCEL_WINDOW_MINUTES
                                + " phút, không thể tự hủy. Vui lòng liên hệ quản lý!");
            }
        }

        // Hoàn kho từng dòng sản phẩm
        AppUser actor = getCurrentUser();
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            stockMovementLogService.logMovement(
                    detail.getVariant(),
                    StockMovementType.CANCEL,
                    detail.getQuantity(),
                    StockRefType.ORDER,
                    order.getId(),
                    "Hủy đơn quầy #" + order.getId(),
                    actor
            );
        }

        // Hoàn lượt dùng voucher (nếu đơn có áp mã)
        orderVoucherRepository.findByOrderId(orderId).ifPresent(ov -> {
            Voucher voucher = ov.getVoucher();
            if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
            }
            order.setOrderVoucher(null);
            orderVoucherRepository.delete(ov);
        });

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelNote((note == null || note.isBlank()) ? null : note.trim());
        order.setCancelledBy(actor);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}