package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.CounterCartItemDTO;
import com.datn.TheCasualWear.entity.AppOrder;
import com.datn.TheCasualWear.entity.AppUser;
import com.datn.TheCasualWear.entity.OrderDetail;
import com.datn.TheCasualWear.entity.ProductVariant;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
import com.datn.TheCasualWear.repository.AppOrderRepository;
import com.datn.TheCasualWear.repository.AppUserRepository;
import com.datn.TheCasualWear.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashierService {

    private final AppOrderRepository       orderRepository;
    private final AppUserRepository        appUserRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Lấy user hiện tại đăng nhập (cashier) — theo đúng pattern dùng
     * SecurityContextHolder như các service khác trong dự án, tránh lỗi
     * @AuthenticationPrincipal trả về null.
     * Dùng findByUsernameOrEmailOrPhone vì đó là method UserDetailsService
     * trong SecurityConfig đang dùng để load user.
     */
    private AppUser getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = ((User) auth.getPrincipal()).getUsername();
        return appUserRepository.findByUsernameOrEmailOrPhone(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập"));
    }

    /**
     * Lấy thông tin 1 variant để hiển thị/thêm vào giỏ tạm (CHƯA trừ kho —
     * kho chỉ bị trừ thật sự lúc checkout()).
     */
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

    /**
     * Tạo đơn hàng tại quầy. Kho chỉ bị trừ tại đây (lúc thanh toán xong),
     * không trừ khi thêm vào giỏ tạm — theo yêu cầu nghiệp vụ đã thống nhất.
     *
     * @param customerId    id khách hàng có tài khoản; null = khách vãng lai
     *                      (không lưu tên/sđt, hóa đơn hiển thị "Khách lẻ")
     * @param items         danh sách sản phẩm trong giỏ tạm
     * @param paymentMethod "CASH" hoặc "TRANSFER"
     */
    @Transactional
    public AppOrder checkout(Integer customerId,
                             List<CounterCartItemDTO> items,
                             String paymentMethod) {

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống!");
        }

        AppUser cashier = getCurrentUser();

        AppOrder order = new AppOrder();
        order.setOrderType(OrderType.COUNTER);
        order.setCashier(cashier);
        order.setStatus(OrderStatus.COMPLETED); // bán tại quầy: khách nhận hàng ngay tại chỗ
        order.setPaymentMethod(paymentMethod);
        order.setIsPaid(true); // thanh toán ngay tại quầy
        order.setOrderDate(LocalDateTime.now());
        order.setDeliveredAt(LocalDateTime.now());

        if (customerId != null) {
            AppUser customer = appUserRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
            order.setCustomer(customer);
        }
        // customerId == null → khách vãng lai, order.customer giữ null

        BigDecimal total = BigDecimal.ZERO;

        for (CounterCartItemDTO item : items) {
            // Re-check tồn kho ngay tại thời điểm thanh toán (chống bán trùng
            // giữa lúc thêm vào giỏ tạm và lúc bấm thanh toán)
            ProductVariant variant = variantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm không tồn tại: " + item.getVariantId()));

            if (variant.getStock() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm '" + variant.getProduct().getName() + "' chỉ còn "
                                + variant.getStock() + " trong kho (giỏ hàng yêu cầu "
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

            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalPrice(total);

        return orderRepository.save(order);
    }

    public AppOrder getOrderForInvoice(Integer orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    /**
     * Lấy danh sách đơn bán tại quầy do cashier hiện tại đăng nhập đã tạo,
     * trong N ngày gần đây (mặc định dùng cho trang "Đơn đã bán").
     */
    public List<AppOrder> getRecentOrdersByCurrentCashier(int days) {
        AppUser cashier = getCurrentUser();
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return orderRepository.findRecentCounterOrdersByCashier(cashier.getId(), from);
    }

    /**
     * Lấy chi tiết 1 đơn quầy. Cashier chỉ xem được đơn do chính mình tạo;
     * admin/owner xem được tất cả (họ cũng có quyền vào /cashier/**).
     */
    public AppOrder getOwnOrderDetail(Integer orderId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_OWNER"));

        AppOrder order = getOrderForInvoice(orderId);
        if (isAdminOrOwner) {
            return order;
        }

        AppUser cashier = getCurrentUser();
        if (order.getCashier() == null || !order.getCashier().getId().equals(cashier.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }
}