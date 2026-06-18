package com.datn.TheCasualWear.service;

import com.datn.TheCasualWear.config.ResourceNotFoundException;
import com.datn.TheCasualWear.dto.OrderListDTO;
import org.springframework.data.domain.PageImpl;
import com.datn.TheCasualWear.entity.*;
import com.datn.TheCasualWear.enums.AssignmentStatus;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AppOrderRepository          orderRepository;
    private final OrderDetailRepository       orderDetailRepository;
    private final OrderVoucherRepository      orderVoucherRepository;
    private final CartService                 cartService;
    private final VoucherService              voucherService;
    private final ProductVariantRepository    variantRepository;
    private final NotificationService         notificationService;
    private final OrderAssignmentRepository   assignmentRepository;
    private final AppUserRepository           appUserRepository;
    private final DeliveryProfileRepository   deliveryProfileRepository;

    private static final int ADMIN_PAGE_SIZE  = 10;
    private static final int MAX_FAIL_COUNT   = 3;
    private static final int RETURN_DAYS      = 15;

    //QUERY

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


    // Lay danh sach order kem failCount va deliveryName cho admin list
    public Page<OrderListDTO> getOrderDTOs(String keyword, String status,
                                           String fromDate, String toDate, int page) {
        Page<AppOrder> orderPage = getAllOrders(keyword, status, fromDate, toDate, page);

        List<OrderListDTO> dtos = orderPage.getContent().stream()
                .map(order -> {
                    int failCount = assignmentRepository.countFailedByOrderId(order.getId());

                    String deliveryName = assignmentRepository
                            .findActiveByOrderId(order.getId())
                            .map(a -> a.getDeliveryStaff().getUsername())
                            .orElse(null);

                    return new OrderListDTO(order, failCount, deliveryName);
                })
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

    // CUSTOMER

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
            voucher        = voucherService.applyVoucher(voucherCode, totalPrice, user);
            BigDecimal discounted = voucherService.calcDiscountedPrice(totalPrice, voucher);
            discountAmount = totalPrice.subtract(discounted);
            totalPrice     = discounted;
        }

        AppOrder order = new AppOrder();
        order.setCustomer(user);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setTotalPrice(totalPrice);
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

            BigDecimal unitPrice = product.getPrice();
            if (variant.getPriceAdjustment() != null) {
                unitPrice = unitPrice.add(variant.getPriceAdjustment());
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setVariant(variant);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(unitPrice);
            orderDetailRepository.save(detail);

            variant.setStock(variant.getStock() - item.getQuantity());
            variantRepository.save(variant);
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

    public void confirmReceived(Integer orderId, AppUser user) {
        AppOrder order = getOrderByIdAndUser(orderId, user);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể xác nhận!");
        }
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Integer orderId, AppUser user) {
        AppOrder order = getOrderByIdAndUser(orderId, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận!");
        }
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            throw new IllegalStateException(
                    "Đơn hàng đã thanh toán không thể hủy trực tiếp. "
                            + "Vui lòng liên hệ Zalo 0901.234.567 để được hỗ trợ!");
        }
        restoreVariantStock(order);
        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.CANCELLED);
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

    // ADMIN

    public void confirmOrder(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ xác nhận!");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được xác nhận! Chúng tôi đang chuẩn bị hàng.",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void cancelOrderByAdmin(Integer orderId) {
        AppOrder order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Không thể hủy đơn hàng này!");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            restoreVariantStock(order);
        }
        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã bị hủy bởi shop. Liên hệ hỗ trợ nếu có thắc mắc.",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void returnOrder(Integer orderId, boolean restock) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ có thể hoàn hàng khi đơn đã giao hoặc hoàn thành!");
        }
        if (order.getDeliveredAt() == null) {
            throw new IllegalStateException("Không xác định được ngày giao hàng!");
        }

        LocalDateTime deadline = order.getDeliveredAt().plusDays(RETURN_DAYS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalStateException(
                    "Đã quá " + RETURN_DAYS + " ngày kể từ khi giao, không thể hoàn hàng!");
        }

        if (restock) {
            restoreVariantStock(order);
        }

        removeOrderVoucher(order, orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được hoàn hàng do sản phẩm lỗi. "
                        + "Shop xin lỗi vì sự bất tiện này!",
                "/order/detail/" + orderId
        );
    }

    // DELIVERY

    @Transactional
    public void markDelivered(Integer orderId, AppUser currentUser) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái đang giao!");
        }

        OrderAssignment assignment = assignmentRepository.findActiveByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Đơn hàng chưa được phân công cho bạn!"));

        if (!assignment.getDeliveryStaff().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Bạn không được phân công giao đơn này!");
        }

        assignment.setStatus(AssignmentStatus.DELIVERED);
        assignment.setDeliveredAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        if ("COD".equals(order.getPaymentMethod())) {
            order.setIsPaid(true);
        }
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer(),
                "Đơn hàng #" + orderId + " đã được giao! Vui lòng xác nhận :>",
                "/order/detail/" + orderId
        );
    }

    @Transactional
    public void markFailed(Integer orderId, AppUser currentUser, String failReason) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái đang giao!");
        }

        OrderAssignment assignment = assignmentRepository.findActiveByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Đơn hàng chưa được phân công!"));

        if (!assignment.getDeliveryStaff().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Bạn không được phân công giao đơn này!");
        }

        assignment.setStatus(AssignmentStatus.FAILED);
        assignment.setFailReason(failReason);
        assignmentRepository.save(assignment);

        int failCount = assignmentRepository.countFailedByOrderId(orderId);

        if (failCount >= MAX_FAIL_COUNT) {
            restoreVariantStock(order);
            removeOrderVoucher(order, orderId);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            notificationService.createNotificationForAdmins(
                    "Đơn hàng #" + orderId + " đã bị hủy tự động sau " + MAX_FAIL_COUNT + " lần giao thất bại!",
                    "/admin/orders/" + orderId
            );
            notificationService.createNotification(
                    order.getCustomer(),
                    "Đơn hàng #" + orderId + " đã bị hủy do giao hàng thất bại "
                            + MAX_FAIL_COUNT + " lần. Vui lòng liên hệ shop để được hỗ trợ!",
                    "/order/detail/" + orderId
            );
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            notificationService.createNotificationForAdmins(
                    "Đơn hàng #" + orderId + " giao thất bại lần " + failCount
                            + "/" + MAX_FAIL_COUNT + "! Lý do: " + failReason,
                    "/admin/orders/" + orderId
            );
            notificationService.createNotification(
                    order.getCustomer(),
                    "Đơn hàng #" + orderId + " giao không thành công. "
                            + "Shop sẽ liên hệ và sắp xếp lại!",
                    "/order/detail/" + orderId
            );
        }
    }

    //ASSIGNMENT

    public Optional<OrderAssignment> getAssignmentByOrderId(Integer orderId) {
        return assignmentRepository.findActiveByOrderId(orderId);
    }

    public List<OrderAssignment> getAssignmentHistory(Integer orderId) {
        return assignmentRepository.findByOrderIdOrderByAssignedAtDesc(orderId);
    }

    public List<OrderAssignment> getMyAssignments(AppUser currentUser,
                                                  String status,
                                                  String fromDate,
                                                  String toDate) {
        AssignmentStatus statusEnum = (status == null || status.isBlank())
                ? null : AssignmentStatus.valueOf(status);

        LocalDateTime from = (fromDate == null || fromDate.isBlank())
                ? LocalDateTime.now().minusDays(7).toLocalDate().atStartOfDay()
                : LocalDate.parse(fromDate).atStartOfDay();

        LocalDateTime to = (toDate == null || toDate.isBlank())
                ? LocalDateTime.now().toLocalDate().atTime(23, 59, 59)
                : LocalDate.parse(toDate).atTime(23, 59, 59);

        return assignmentRepository.findByDeliveryStaffFiltered(
                currentUser.getId(), statusEnum, from, to);
    }

    @Transactional
    public void assignOrder(Integer orderId, Integer deliveryId, AppUser assignedBy) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ có thể giao đơn đang ở trạng thái đã xác nhận!");
        }
        if (assignmentRepository.existsActiveByOrderId(orderId)) {
            throw new IllegalStateException(
                    "Đơn hàng này đang được giao bởi nhân viên khác!");
        }

        int failCount = assignmentRepository.countFailedByOrderId(orderId);
        if (failCount >= MAX_FAIL_COUNT) {
            throw new IllegalStateException(
                    "Đơn hàng này đã thất bại " + MAX_FAIL_COUNT + " lần, không thể giao thêm!");
        }

        AppUser delivery = appUserRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên!"));

        // Kiem tra delivery co available khong
        deliveryProfileRepository.findByUserId(deliveryId).ifPresent(p -> {
            if (!Boolean.TRUE.equals(p.getIsAvailable())) {
                throw new IllegalStateException("Nhân viên này hiện không nhận đơn!");
            }
        });

        OrderAssignment assignment = OrderAssignment.builder()
                .order(order)
                .deliveryStaff(delivery)
                .assignedBy(assignedBy)
                .status(AssignmentStatus.ASSIGNED)
                .build();
        assignmentRepository.save(assignment);

        // Tu dong chuyen sang SHIPPING khi assign
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);

        notificationService.createNotification(
                delivery,
                "Bạn được phân công giao đơn hàng #" + orderId
                        + "! Địa chỉ: " + order.getShippingAddress().getStreet()
                        + ", " + order.getShippingAddress().getCity(),
                "/delivery/orders/" + orderId
        );
    }


    // Admin doi nguoi giao, khong tinh fail
    @Transactional
    public void reassignOrder(Integer orderId, Integer newDeliveryId, AppUser assignedBy) {
        AppOrder order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPING
                && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ có thể reassign khi đơn đang SHIPPING hoặc CONFIRMED!");
        }

        // Set assignment dang active -> CANCELLED (khong tinh failCount)
        assignmentRepository.findActiveByOrderId(orderId).ifPresent(a -> {
            a.setStatus(AssignmentStatus.CANCELLED);
            assignmentRepository.save(a);
        });

        AppUser newDelivery = appUserRepository.findById(newDeliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhân viên!"));

        deliveryProfileRepository.findByUserId(newDeliveryId).ifPresent(p -> {
            if (!Boolean.TRUE.equals(p.getIsAvailable())) {
                throw new IllegalStateException("Nhân viên này hiện không nhận đơn!");
            }
        });

        OrderAssignment newAssignment = OrderAssignment.builder()
                .order(order)
                .deliveryStaff(newDelivery)
                .assignedBy(assignedBy)
                .status(AssignmentStatus.ASSIGNED)
                .build();
        assignmentRepository.save(newAssignment);

        // Dam bao order o SHIPPING
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);

        notificationService.createNotification(
                newDelivery,
                "Bạn được phân công giao đơn hàng #" + orderId
                        + "! Địa chỉ: " + order.getShippingAddress().getStreet()
                        + ", " + order.getShippingAddress().getCity(),
                "/delivery/orders/" + orderId
        );
    }

    //SCHEDULED

    @Transactional
    public void autoConfirmDeliveredOrders() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        orderRepository.findByStatus(OrderStatus.DELIVERED).stream()
                .filter(o -> o.getDeliveredAt() != null
                        && o.getDeliveredAt().isBefore(twoDaysAgo))
                .forEach(o -> {
                    o.setStatus(OrderStatus.COMPLETED);
                    orderRepository.save(o);
                    notificationService.createNotification(
                            o.getCustomer(),
                            "Đơn hàng #" + o.getId()
                                    + " đã được tự động xác nhận hoàn thành. Cảm ơn bạn đã mua hàng! =))",
                            "/order/detail/" + o.getId()
                    );
                });
    }

    @Transactional
    public void deleteCancelledOrderAfterMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        orderRepository.findByStatus(OrderStatus.CANCELLED).stream()
                .filter(o -> o.getOrderDate().isBefore(oneMonthAgo))
                .forEach(orderRepository::delete);
    }

    //HELPER
    @Transactional
    protected void restoreVariantStock(AppOrder order) {
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            ProductVariant variant = detail.getVariant();
            variant.setStock(variant.getStock() + detail.getQuantity());
            variantRepository.save(variant);
        }
    }

    private void removeOrderVoucher(AppOrder order, Integer orderId) {
        orderVoucherRepository.findByOrderId(orderId).ifPresent(ov -> {
            order.setOrderVoucher(null);
            orderVoucherRepository.delete(ov);
        });
    }
}