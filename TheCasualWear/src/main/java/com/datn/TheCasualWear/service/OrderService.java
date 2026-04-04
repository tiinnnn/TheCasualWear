package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final AppOrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderVoucherRepository orderVoucherRepository;
    private final CartService cartService;
    private final VoucherService voucherService;
    private final ProductRepository productRepository;
    private static final int ADMIN_PAGE_SIZE = 10;


    public OrderService(AppOrderRepository orderRepository,
                        OrderDetailRepository orderDetailRepository,
                        OrderVoucherRepository orderVoucherRepository,
                        CartService cartService,
                        VoucherService voucherService,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.orderVoucherRepository = orderVoucherRepository;
        this.cartService = cartService;
        this.voucherService = voucherService;
        this.productRepository = productRepository;
    }


    public Page<AppOrder> getAllOrders(String keyword, String status, int page) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String st = (status == null || status.isBlank()) ? null : status;
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE);
        return orderRepository.searchOrders(kw, st, pageable);
    }

    public AppOrder getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));
    }

    // Kiểm tra đơn hàng có thuộc về user không
    public AppOrder getOrderByIdAndUser(Integer id, AppUser user) {
        AppOrder order = getOrderById(id);
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }

    // ==================== PHÍA CUSTOMER ====================

    // Đặt hàng
    @Transactional
    public AppOrder placeOrder(AppUser user, Address shippingAddress,
                               Address billingAddress, String voucherCode) {
        // Lấy giỏ hàng
        List<CartItem> cartItems = cartService.getCartItems(user);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống!");
        }

        BigDecimal totalPrice = BigDecimal.valueOf(cartService.getTotalPrice(user));

        // Áp dụng voucher nếu có
        Voucher voucher = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherService.applyVoucher(voucherCode, totalPrice, user);
            totalPrice = voucherService.calcDiscountedPrice(totalPrice, voucher);
        }

        // Tạo đơn hàng
        AppOrder order = new AppOrder();
        order.setCustomer(user);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Kiểm tra stock lần cuối trước khi đặt
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Sản phẩm '"
                        + product.getName() + "' chỉ còn " + product.getStock() + " trong kho!");
            }

            // Tạo order detail
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(product.getPrice()); // lưu giá tại thời điểm mua
            orderDetailRepository.save(detail);

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // Lưu voucher đã dùng
        if (voucher != null) {
            OrderVoucher orderVoucher = new OrderVoucher();
            orderVoucher.setOrder(order);
            orderVoucher.setVoucher(voucher);
            orderVoucher.setCustomer(user);
            orderVoucherRepository.save(orderVoucher);
        }

        // Xóa giỏ hàng
        cartService.clearCart(user);

        return order;
    }

    // Lịch sử đơn hàng của customer
    public List<AppOrder> getOrdersByUser(AppUser user) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(user.getId());
    }

    public void confirmReceived(Integer orderId, AppUser user) {
        AppOrder order = getOrderByIdAndUser(orderId, user);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể xác nhận!");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    // cancelOrder (customer)
    @Transactional
    public void cancelOrder(Integer orderId, AppUser user) {
        AppOrder order = getOrderByIdAndUser(orderId, user);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận!");
        }

        restoreStock(order);

        // Xóa OrderVoucher để customer dùng lại được
        orderVoucherRepository.findByOrderId(orderId)
                .ifPresent(orderVoucherRepository::delete);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // ==================== PHÍA ADMIN ====================

    public List<AppOrder> getAllOrders() {
        return orderRepository.findAllOrderedByStatus();
    }

    public List<AppOrder> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // PENDING → CONFIRMED
    public void confirmOrder(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ xác nhận!");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    // CONFIRMED → SHIPPING
    public void shipOrder(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Đơn hàng chưa được xác nhận!");
        }
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
    }

    // Admin hủy đơn
    @Transactional
    public void cancelOrderByAdmin(Integer orderId) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Không thể hủy đơn hàng này!");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            restoreStock(order);
        }

        // Xóa OrderVoucher để customer dùng lại được
        orderVoucherRepository.findByOrderId(orderId)
                .ifPresent(orderVoucherRepository::delete);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // ==================== PHÍA DELIVERY ====================

    // Lấy đơn hàng đang giao
    public List<AppOrder> getShippingOrders() {
        return orderRepository.findByStatus(OrderStatus.SHIPPING);
    }

    // SHIPPING → DELIVERED
    public void markDelivered(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái đang giao!");
        }
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    // ==================== HELPER ====================

    // Hoàn lại stock khi hủy đơn
    private void restoreStock(AppOrder order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            Product product = detail.getProduct();
            product.setStock(product.getStock() + detail.getQuantity());
            productRepository.save(product);
        }
    }
}