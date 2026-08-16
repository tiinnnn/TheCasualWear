package com.datn.TheCasualWear.entity;

import com.datn.TheCasualWear.enums.CancelReason;
import com.datn.TheCasualWear.enums.OrderStatus;
import com.datn.TheCasualWear.enums.OrderType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_order")
public class AppOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ── MỚI: mã tra cứu công khai cho khách vãng lai (4.2, 6.6) ───────────
    // Sinh ngẫu nhiên lúc tạo đơn (xem OrderCodeGenerator), KHÔNG dùng id
    // tự tăng để tránh enumeration attack. Dùng để tra cứu + hiển thị trên
    // email xác nhận, không thay thế id nội bộ (POS/admin/GHN vẫn dùng id).
    @Column(name = "order_code", length = 12, unique = true, nullable = false)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private AppUser customer; // null nếu là đơn khách vãng lai

    // ── MỚI: email khách vãng lai (4.1) ────────────────────────────────────
    // SĐT khách vãng lai tái dùng shippingAddress.phone (đã có sẵn, not null).
    // Email không có chỗ lưu tự nhiên trên Address nên thêm riêng ở đây,
    // đồng thời đóng vai trò snapshot cho việc tra cứu đơn (4.2/6.6) dù
    // địa chỉ giao hàng có bị sửa sau này. Null nếu là đơn của user đã đăng nhập.
    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(name = "order_date", updatable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_price", precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderVoucher orderVoucher;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "COD"; // COD hoặc VNPAY

    @Column(name = "is_paid")
    private Boolean isPaid = false; // VNPay → true ngay, COD → true khi delivered

    // ── MỚI: tích hợp bên thứ 3 (GHN / GHTK ...) ──────────────────────────

    @Column(name = "tracking_code", length = 50)
    private String trackingCode;   // Mã vận đơn GHN nhân viên nhập thủ công

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt; // Thời điểm admin xác nhận gửi hàng cho GHN

    // ── MỚI: bán hàng tại quầy (Cashier / POS) ────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20, nullable = false)
    private OrderType orderType = OrderType.ONLINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private AppUser cashier; // Nhân viên thu ngân tạo đơn (chỉ có khi order_type = COUNTER)

    // ── MỚI: audit lý do hủy/hoàn — chỉ có giá trị khi status = CANCELLED
    // hoặc RETURNED. Dùng chung cho cả 2 trạng thái vì đều là hành động
    // "kết thúc đơn hàng ngoài dự kiến", chỉ khác thời điểm xảy ra.

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private CancelReason cancelReason;

    @Column(name = "cancel_note", length = 255)
    private String cancelNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private AppUser cancelledBy; // Ai thực hiện: customer tự hủy, admin, hoặc cashier

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}