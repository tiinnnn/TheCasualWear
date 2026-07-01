package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.OrderDetail;
import com.datn.TheCasualWear.entity.OrderVoucher;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.entity.Voucher;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
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

        BigDecimal unitPrice = variant.getProduct().getPrice();
        if (variant.getPriceAdjustment() != null) {
            unitPrice = unitPrice.add(variant.getPriceAdjustment());
        }

        return new CounterCartItemDTO(
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSize() != null ? variant.getSize().getName() : null,
                variant.getColor() != null ? variant.getColor().getName() : null,
                variant.getSku(),
                unitPrice,
                quantity,
                variant.getStock()
        );
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
    // CHECKOUT — trừ kho + áp voucher (nếu có) + tạo AppOrder
    //
    // Toàn bộ nằm trong 1 @Transactional:
    //   trừ kho → validate & trừ usedCount voucher → save order
    // Nếu bất kỳ bước nào fail → rollback toàn bộ, không mất kho / lượt voucher.
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

        if (customerId != null) {
            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
            order.setCustomer(customer);
        }

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

            variant.setStock(variant.getStock() - item.getQuantity());
            variantRepository.save(variant);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice());
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

    public AppOrder getOwnOrderDetail(Integer orderId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrOwner = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_OWNER"));

        AppOrder order = getOrderForInvoice(orderId);
        if (isAdminOrOwner) return order;

        AppUser cashier = getCurrentUser();
        if (order.getCashier() == null || !order.getCashier().getId().equals(cashier.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }
}